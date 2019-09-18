/*
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package com.amazon.disco.agent.context;

import com.amazon.disco.agent.concurrent.TransactionContext;
import java.util.HashSet;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class ContextPropagationUtilTest {

    @Test
    public void testPackPropagationAttributesWithoutSpecialCharacters() {
        TransactionContext.clear();
        TransactionContext.putMetadata("KeyA", "ValueA");
        TransactionContext.putMetadata("KeyB", "ValueB");
        TransactionContext.setMetadataTag("KeyA", TransactionContext.PROPAGATE_IN_REQUEST_TAG);
        TransactionContext.setMetadataTag("KeyB", TransactionContext.PROPAGATE_IN_REQUEST_TAG);
        String packedAttributes = ContextPropagationUtil.packPropagationAttributeForHeaderFromContext();
        Assert.assertEquals("KeyB=ValueB&KeyA=ValueA", packedAttributes);
    }

    @Test
    public void testPackPropagationAttributesWithoutSpecialCharactersAndWhiteList() {
        TransactionContext.clear();
        TransactionContext.putMetadata("KeyA", "ValueA");
        TransactionContext.putMetadata("KeyB", "ValueB");
        TransactionContext.setMetadataTag("KeyA", TransactionContext.PROPAGATE_IN_REQUEST_TAG);
        TransactionContext.setMetadataTag("KeyB", TransactionContext.PROPAGATE_IN_REQUEST_TAG);

        HashSet<String> whiteListHeaders = new HashSet<>();
        whiteListHeaders.add("KeyB");
        String packedAttributes = ContextPropagationUtil.packPropagationAttributeForHeaderFromContext(whiteListHeaders);
        Assert.assertEquals("KeyA=ValueA", packedAttributes);
        Assert.assertEquals("ValueB", TransactionContext.getMetadata("KeyB"));
    }

    @Test
    public void testGetWhiteListAttributesFromContext() {
        HashSet<String> whiteListHeaders = new HashSet<>();
        whiteListHeaders.add("KeyB");
        TransactionContext.clear();
        TransactionContext.putMetadata("KeyA", "ValueA");
        TransactionContext.putMetadata("KeyB", "ValueB");
        TransactionContext.setMetadataTag("KeyA", TransactionContext.PROPAGATE_IN_REQUEST_TAG);
        TransactionContext.setMetadataTag("KeyB", TransactionContext.PROPAGATE_IN_REQUEST_TAG);

        Map<String, Object> whiteListAttributes = ContextPropagationUtil.getWhiteListAttributesFromContext(whiteListHeaders);
        Assert.assertEquals( 1, whiteListAttributes.size());
        Assert.assertEquals("ValueB", whiteListAttributes.get("KeyB"));
    }

    @Test
    public void testUnpackPropagationAttributesWithoutSpecialCharacters() {
        TransactionContext.clear();
        ContextPropagationUtil.unpackPropagationHeaderIntoContext("KeyB=ValueB&KeyA=ValueA");
        Assert.assertEquals("ValueA", TransactionContext.getMetadata("KeyA"));
        Assert.assertEquals("ValueB", TransactionContext.getMetadata("KeyB"));
    }

    @Test
    public void testPackPropagationAttributesWithSpecialCharacters() {
        TransactionContext.clear();
        TransactionContext.putMetadata("KeyA", "Value_&A");
        TransactionContext.putMetadata("KeyB", "Value,@B");
        TransactionContext.setMetadataTag("KeyA", TransactionContext.PROPAGATE_IN_REQUEST_TAG);
        TransactionContext.setMetadataTag("KeyB", TransactionContext.PROPAGATE_IN_REQUEST_TAG);

        String packedAttributes = ContextPropagationUtil.packPropagationAttributeForHeaderFromContext();
        Assert.assertEquals("KeyB=Value%2C%40B&KeyA=Value_%26A", packedAttributes);
    }

    @Test
    public void testUnpackPropagationAttributesWitSpecialCharacters() {
        TransactionContext.clear();
        ContextPropagationUtil.unpackPropagationHeaderIntoContext("KeyB=Value%2C%40B&KeyA=Value_%26A");
        Assert.assertEquals("Value_&A", TransactionContext.getMetadata("KeyA"));
        Assert.assertEquals("Value,@B", TransactionContext.getMetadata("KeyB"));
    }
}
