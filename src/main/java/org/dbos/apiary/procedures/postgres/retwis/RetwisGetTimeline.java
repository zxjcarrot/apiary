package org.dbos.apiary.procedures.postgres.retwis;

import org.dbos.apiary.function.ApiaryTransactionalContext;
import org.dbos.apiary.postgres.PostgresFunction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class RetwisGetTimeline extends PostgresFunction {

    public static String[] runFunction(ApiaryTransactionalContext ctxt, int userID) throws SQLException {
        int[] followees = ctxt.apiaryCallFunction("org.dbos.apiary.procedures.postgres.retwis.RetwisGetFollowees", userID).getIntArray();
        List<String> posts = new ArrayList<>();
        for (int followee: followees) {
            String[] userPosts = ctxt.apiaryCallFunction("org.dbos.apiary.procedures.postgres.retwis.RetwisGetPosts", followee).getStringArray();
            posts.addAll(Arrays.asList(userPosts));
        }
        return posts.toArray(new String[0]);
    }
}
