package org.jmingo.context;


import com.mongodb.Mongo;
import com.mongodb.WriteConcern;
import org.jmingo.context.Context;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

public class ContextTest {

    @Test
    public void testContext() {
        Context context = null;
        try {
            context = Context.create("/xml/context.xml");
            Mongo mongo = context.getMongoDBFactory().getMongo();
            assertEquals(WriteConcern.ACKNOWLEDGED, mongo.getWriteConcern());
            assertEquals(100, mongo.getMongoOptions().getConnectTimeout());
        } finally {
            if (context != null) {
                context.shutdown();
            }
        }
    }

    @Test
    public void testEmptyContext() {
        Context context = null;
        try {
            context = Context.create("/xml/emptyContext.xml");
        } finally {
            if (context != null) {
                context.shutdown();
            }
        }
    }

}
