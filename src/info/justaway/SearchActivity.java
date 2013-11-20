package info.justaway;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TableLayout;

import java.util.List;

import info.justaway.adapter.TwitterAdapter;
import info.justaway.model.Row;
import twitter4j.Query;
import twitter4j.QueryResult;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.StatusUpdate;
import twitter4j.Twitter;

public class SearchActivity extends Activity {

    private Context context;
    private Twitter twitter;
    private EditText searchWords;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);
        context = this;

        JustawayApplication application = JustawayApplication.getApplication();
        twitter = application.getTwitter();

        Button search = (Button) findViewById(R.id.search);
        search.setTypeface(Typeface.createFromAsset(context.getAssets(), "fontello.ttf"));

        searchWords = (EditText) findViewById(R.id.searchWords);

        findViewById(R.id.search).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Query query = new Query(searchWords.getText().toString());
                new SearchTask().execute(query);
            }
        });
    }

    private class SearchTask extends AsyncTask<Query, Void, QueryResult> {
        @Override
        protected QueryResult doInBackground(Query... params) {
            Query query = params[0];
            try {
                QueryResult queryResult = twitter.search(query);
                return queryResult;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(QueryResult queryResult) {
            TableLayout table = (TableLayout) findViewById(R.id.table);
            TwitterAdapter adapter = new TwitterAdapter(context, R.layout.row_tweet_for_table);
            List<twitter4j.Status> statuses = queryResult.getTweets();
            int i = 0;
            for (twitter4j.Status status : statuses) {
                adapter.add(Row.newStatus(status));
                View row = adapter.getView(i, null, table);
                table.addView(row);
                i++;
            }
        }
    }
}