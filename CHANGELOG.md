# Change Log

## Version 0.11.0 - 04/07/2021

* Added additional SQL interception support for prepared statements and calls [PR #16](https://github.com/awslabs/disco/pull/16)

## Version 0.10.0 - 08/25/2020

* Added SQL interception package [PR #10](https://github.com/awslabs/disco/pull/10)
* Added AWS interception package [PR #10](https://github.com/awslabs/disco/pull/10)
* Added instrumentation preprocess package [PR #10](https://github.com/awslabs/disco/pull/10)
* Added Bill of Materials [PR #10](https://github.com/awslabs/disco/pull/10)
* Added installable `Package` class for collections of installables [PR #10](https://github.com/awslabs/disco/pull/10)
* Added concurrency support for `ScheduledThreadPoolExecutor` [PR #10](https://github.com/awslabs/disco/pull/10)
* Added Service downstream cancellation events [PR #10](https://github.com/awslabs/disco/pull/10)
* Added `HeaderReplaceable` interface for event classes [PR #10](https://github.com/awslabs/disco/pull/10)
* Added `removeMetadata` method for Transaction Context [PR #10](https://github.com/awslabs/disco/pull/10)
* Added support in core package for preprocess build tool [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed deprecated reflective access in `ForkJoinTask` tests [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed null pointer issues in `HttpResponseEvent` and `TransactionContext` [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed `ExecutorService` to use re-entrancy check [PR #10](https://github.com/awslabs/disco/pull/10)
* Fixed flaky TX tests [PR #10](https://github.com/awslabs/disco/pull/10)
* Ensure transaction context is propagated for nested executor submissions [PR #10](https://github.com/awslabs/disco/pull/10)
* Deprecated `MethodHandleWrapper` class [PR #10](https://github.com/awslabs/disco/pull/10)
* Upgraded ByteBuddy to 1.10.14 and ASM to 8.0.1 [PR #10](https://github.com/awslabs/disco/pull/10)
* Upgraded to Gradle 6.6 [PR #10](https://github.com/awslabs/disco/pull/10)

## Version 0.9.1 - 12/2/2019

* Initial commit of DiSCo Toolkit
