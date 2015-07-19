package org.wordpress.android.ui.chat;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;

import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import it.gmariotti.cardslib.library.internal.Card;
import it.gmariotti.cardslib.library.internal.CardGridArrayAdapter;
public class ChatActivity extends ActionBarActivity {
    ListView gridView;
    private ArrayList<Card> cards;
    CardGridArrayAdapter mCardArrayAdapter;
    List<Message> messagesList;

    private GifAnimationDrawable little;

    private SharedPreferences pref;
    private String user_id;
    private static final String USER_ID = "user_id";
    private static final String USER_NAME = "user_name";

    private MenuItem friendIcon = null;
    private Drawable friendDrawable;

    private EditText chatText;
    private ImageView buttonSend;
    private ChatArrayAdapter adClass;
    private String friend_id;
    private WordPressDB db;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);

        gridView = (ListView) findViewById(R.id.messagesList);

        chatText = (EditText)findViewById(R.id.chatText);
        buttonSend = (ImageView)findViewById(R.id.buttonSend);

        db = WordPress.wpDB;

        messagesList = db.getMessages();

        adClass = new ChatArrayAdapter(this, messagesList);
        gridView.setAdapter(adClass);

        chatText.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if ((event.getAction() == KeyEvent.ACTION_DOWN) && (keyCode == KeyEvent.KEYCODE_ENTER)) {
                    return sendChatMessage("" + chatText.getText().toString());
                }
                return false;
            }
        });

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View arg0) {
                sendChatMessage("" + chatText.getText().toString());
            }
        });
    }

    public boolean sendChatMessage(String message_text){

        message_text = message_text.trim();

        Message message = new Message();
        message.setMessage(message_text);
        message.setIsMine("1");

        db.addMessage(message);

        new SendMessage(this, user_id, message.getMessage()).execute();

        chatText.setText("");
        adClass.add(message);

        return true;
    }



    public class ChatArrayAdapter  extends ArrayAdapter<Message> {
        Context context;
        private List<Message> TextValue;

        public ChatArrayAdapter(Context context, List<Message> TextValue) {
            super(context, R.layout.drawer_list_footer_row, TextValue);
            this.context = context;
            this.TextValue= TextValue;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub

            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);


            ViewHolder holder;
            if(convertView == null)
            {
                holder = new ViewHolder();
                convertView = inflater.inflate(R.layout.messages_row, parent, false);
                holder.message = (TextView) convertView.findViewById(R.id.message_text);
                convertView.setTag(holder);
            }
            else
                holder = (ViewHolder) convertView.getTag();

            holder.message.setText(messagesList.get(position).getMessage());

            LayoutParams lp = (LayoutParams) holder.message.getLayoutParams();
            //check if it is a status message then remove background, and change text color.

            //Check whether message is mine to show green background and align to right
            if(messagesList.get(position).getIsMine().equals("1"))
            {
                holder.message.setBackgroundResource(R.drawable.bubble_a);
                lp.gravity = Gravity.RIGHT;
            }
            //If not mine then it is from sender to show orange background and align to left
            else
            {
                holder.message.setBackgroundResource(R.drawable.bubble_b);
                lp.gravity = Gravity.LEFT;
            }

            holder.message.setLayoutParams(lp);
            //holder.message.setTextColor(getResources().getColor(R.color.counter_text_color));

            return convertView;

        }
        private class ViewHolder
        {
            TextView message;
        }

        @Override
        public long getItemId(int position) {
            //Unimplemented, because we aren't using Sqlite.
            return position;
        }

    }

}