/*
 * Copyright 2021 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License").
 *   You may not use this file except in compliance with the License.
 *   A copy of the License is located at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   or in the "license" file accompanying this file. This file is distributed
 *   on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *   express or implied. See the License for the specific language governing
 *   permissions and limitations under the License.
 */

package software.amazon.disco.agent.integtest

import kotlinx.coroutines.*
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import org.junit.Assert.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import software.amazon.disco.agent.reflect.concurrent.TransactionContext
import java.util.concurrent.atomic.AtomicBoolean


class KotlinCoroutinesInterceptorTest {

    private var fixedDispatcher: CoroutineDispatcher? = null

    @BeforeEach
    fun before() {
        TransactionContext.create()
        fixedDispatcher = newFixedThreadPoolContext(4, "test-disptacher")
    }

    @AfterEach
    fun after() {
        TransactionContext.destroy()
    }

    @Test
    fun testRunBlockingShouldHaveTheSameValueInTransactionContext() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking (fixedDispatcher!!) {
            assertEquals("test-value", TransactionContext.getMetadata("test-id") )
        }
    }

    @Test
    fun testLaunchShouldHaveTheSameValueInTransactionContext() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking {
            val isPropagated = AtomicBoolean(false)


            // We explicitly specify that the coroutine should be executed in the GlobalScope so that the coroutine
            // is not a child of the coroutine built by runBlocking.
            // This is to ensure that the coroutine scope is set by intercepting launch, rather than simply inheriting from its parent.
            GlobalScope.launch(fixedDispatcher!!) {
                isPropagated.set("test-value" == TransactionContext.getMetadata("test-id"))
            }.join()

            assertTrue("TX propagation failed for launch use case", isPropagated.get())
        }
    }

    @Test
    fun testAsyncShouldHaveTheSameValueInTransactionContext() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking {
            val isPropagated = AtomicBoolean(false)

            // We explicitly specify that the coroutine should be executed in the GlobalScope so that the coroutine
            // is not a child of the coroutine built by runBlocking.
            // This is to ensure that the coroutine scope is set by intercepting async, rather than simply inheriting from its parent.
            GlobalScope.async(fixedDispatcher!!) {
                isPropagated.set("test-value" == TransactionContext.getMetadata("test-id"))
            }.await()

            assertTrue("TX propagation failed for async use case", isPropagated.get())
        }
    }

    @Test
    fun testFutureShouldHaveTheSameValueInTransactionContext() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking {
            val isPropagated = AtomicBoolean(false)

            // We explicitly specify that the coroutine should be executed in the GlobalScope so that the coroutine
            // is not a child of the coroutine built by runBlocking.
            // This is to ensure that the coroutine scope is set by intercepting future, rather than simply inheriting from its parent.
            GlobalScope.future(fixedDispatcher!!) {
                isPropagated.set("test-value" == TransactionContext.getMetadata("test-id"))
            }.await()

            assertTrue("TX propagation failed for future use case", isPropagated.get())
        }
    }

    @Test
    fun testLaunchShouldHaveTheCorrectTXMetadataIfItIsRunningUnconfined() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking (Dispatchers.Unconfined) {
            val job = launch (Dispatchers.Unconfined, start = CoroutineStart.LAZY) {
                assertEquals("test-value", TransactionContext.getMetadata("test-id") )
            }
            TransactionContext.destroy()
            job.start()
        }
    }

    @Test
    fun testLaunchShouldHaveTheCorrectTXMetadataIfDestroyedFromParentScope() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking (fixedDispatcher!!) {
            val job = launch (fixedDispatcher!!, start = CoroutineStart.LAZY) {
                assertEquals("test-value", TransactionContext.getMetadata("test-id") )
            }
            TransactionContext.destroy()
            job.start()
        }
    }

    @Test
    fun testAsyncShouldHaveTheCorrectTXMetadataWhenDestroyedFromParentScope() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking (Dispatchers.Default) {
            val job = async (fixedDispatcher!!, start = CoroutineStart.LAZY) {
                assertEquals("test-value", TransactionContext.getMetadata("test-id") )
            }
            TransactionContext.destroy()
            assertNull(TransactionContext.getMetadata("test-id"))
            job.start()
        }
    }

    @Test
    fun testLaunchShouldHaveTheCorrectTXMetadataIfChangedFromParentScope() {
        TransactionContext.putMetadata("test-id", "test-value")
        runBlocking (fixedDispatcher!!) {
            val job = launch (fixedDispatcher!!) {
                assertEquals("other-value", TransactionContext.getMetadata("test-id") )
            }
            TransactionContext.putMetadata("test-id", "other-value")
            job.start()
        }
    }

    @Test
    fun testWithCoroutineContextIsInheritingTXThreadLocalElement() {
        runBlocking {
            TransactionContext.putMetadata("key1", "value1")
            val isPropagated = AtomicBoolean(false)

            withContext(fixedDispatcher!!) { // withContext will finish running first.
                isPropagated.set("value1" == TransactionContext.getMetadata("key1"))
                TransactionContext.putMetadata("key2", "value2")
            }

            assertTrue("TX propagation failed for withContext ",
                    isPropagated.get() && TransactionContext.getMetadata("key2") == "value2")
        }
    }

    @Test
    fun testMultipleRequestsAndTXUpdatesShouldBeIsolatedWithSameStaticDispatcher() {
        TransactionContext.clear()

        val dispatcher = newSingleThreadContext("single-test-thread") // static dispatcher containing single thread.
        val maxThreads = 10
        val requestPool = arrayOfNulls<Thread>(maxThreads)
        val requestPropagation = Array(maxThreads) { AtomicBoolean(true) }

        for (requestId in 0 until maxThreads) {
            requestPool[requestId] = createRequest(requestId, requestPropagation, dispatcher)
            requestPool[requestId]?.start()
        }

        for (requestId in 0 until maxThreads) {
            requestPool[requestId]?.join()
            assertTrue("Thread $requestId has failed assertion for TX propagation" , requestPropagation[requestId].get())
        }
    }

    private fun createRequest(requestId: Int, requestPropagation: Array<AtomicBoolean>, dispatcher: ExecutorCoroutineDispatcher): Thread {
        return Thread { // create a new thread simulating a new request.
            TransactionContext.create()
            runBlocking(dispatcher) { // run coroutines on the same static dispatcher
                launch(dispatcher) {
                    TransactionContext.putMetadata(requestId.toString(), "value")
                }.join()

                for (j in requestPropagation.indices) {
                    // Check that only the current requestId metadata is propagated correctly, otherwise null for any other keys.
                    val value: String? = if (requestId == j) "value" else null
                    requestPropagation[requestId].compareAndSet(true, TransactionContext.getMetadata(j.toString()) == value)
                }
            }
            TransactionContext.destroy()
        }
    }

}