package org.jointsecurityarea.PuffinFeeder;

import android.os.AsyncTask;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Base64;
import android.util.Log;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ViewFeed extends FragmentActivity
{
    private static final int NUM_PAGES = 10;
    private ViewPager viewPager;
    private MyPagerAdapter pagerAdapter;
    private static final String FEED = "http://feeder.gina.alaska.edu/npp-gina-alaska-truecolor-images.xml";


    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager_layout);
        viewPager = (ViewPager) findViewById(R.id.viewpager);
        try {
            pagerAdapter = new MyPagerAdapter(getSupportFragmentManager());
        } catch (XmlPullParserException e) {
            Log.e("Puffin", "Error parsing XML");
        } catch (IOException e) {
            Log.e("Puffin", "Error parsing or opening URL");
        }
        viewPager.setAdapter(pagerAdapter);
    }

    private class MyPagerAdapter extends FragmentPagerAdapter {

        ArrayList<PuffinItem> puffinItems;

        MyPagerAdapter(FragmentManager fm) throws XmlPullParserException, IOException {
            super(fm);
            puffinItems = new ArrayList<PuffinItem>();
            new GetRssTask().execute(FEED);
        }

        @Override
        public Fragment getItem(int i) {
            return PageFragment.newInstance(puffinItems.get(i));
        }

        @Override
        public int getCount() {
            return puffinItems.size();
        }

        class GetRssTask extends AsyncTask<String, Void, String> {
            @Override
            protected String doInBackground(String... urls) {
                HttpClient client = new DefaultHttpClient();
                String content = "";
                try {
                    HttpGet get = new HttpGet(urls[0]);
                    HttpResponse response = client.execute(get);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
                    String line = null;
                    while(((line = reader.readLine()) != null)) {
                        content += line + System.getProperty("line.separator");
                    }
                } catch (ClientProtocolException e) {
                    Log.e("Puffin", "Protocol exception: " + e.getLocalizedMessage());
                    return content;
                } catch (IOException e) {
                    Log.e("Puffin", "IO exception: " + e.getLocalizedMessage());
                    return content;
                }
                return content;
            }

            @Override
            protected void onPostExecute(String input) {
                if(input == null) {
                    Log.e("Puffin", "XML stream is null");
                    return;
                }
                XmlPullParserFactory factory = null;
                try {
                    factory = XmlPullParserFactory.newInstance();
                } catch (XmlPullParserException e) {
                    Log.e("Puffin", "Error getting new pull parser instance: " + e.getLocalizedMessage());
                }
                factory.setNamespaceAware(false);
                XmlPullParser xpp = null;
                try {
                    xpp = factory.newPullParser();
                } catch (XmlPullParserException e) {
                    Log.e("Puffin", "Error making a new pull parser: " + e.getLocalizedMessage());
                }

                try {
                    StringReader reader = new StringReader(input);
                    xpp.setInput(reader);
                } catch (XmlPullParserException e) {
                    Log.e("Puffin", "Error setting instream for xml" + e.getLocalizedMessage());
                }


                boolean insideItem = false;
                int eventType = 0;
                try {
                    eventType = xpp.getEventType();
                } catch (XmlPullParserException e) {
                    Log.e("Puffin", "Failed to get event type");
                }
                PuffinItem pi = new PuffinItem();
                while(eventType != XmlPullParser.END_DOCUMENT) {
                    if(eventType == XmlPullParser.START_TAG) {
                        if(xpp.getName().equalsIgnoreCase("item"))
                            insideItem = true;
                        else if(insideItem) {
                            if(xpp.getName().equalsIgnoreCase("title")) {
                                try {
                                    pi.title = xpp.nextText();
                                } catch (Exception e) {
                                    Log.e("Puffin", "Failed to get title text");
                                }
                            }
                            else if(xpp.getName().equalsIgnoreCase("link")) {
                                try {
                                    pi.link = xpp.nextText();
                                } catch (Exception e) {
                                    Log.e("Puffin", "Failed to get link text");
                                }
                            }
                            else if(xpp.getName().equalsIgnoreCase("image")) {
                                try {
                                    // Deal with totally broken links
                                    String url = xpp.nextText();
                                    Pattern pattern = Pattern.compile("(.*/media/)([^/]+)(.*)");
                                    Matcher m = pattern.matcher(url);
                                    String b64 = "";
                                    String prefix = "";
                                    String suffix = "";
                                    if(m.find()) {
                                        prefix = m.group(1);
                                        b64 = m.group(2);
                                        suffix = m.group(3);
                                    }
                                    byte[] decoded = Base64.decode(b64, Base64.DEFAULT);
                                    JSONArray array = new JSONArray(new String(decoded));
                                    JSONArray a2 = new JSONArray();
                                    a2.put(array.get(0));
                                    array = a2;
                                    //JSONArray params = array.getJSONArray(1);
                                    //params.put(2, "3000x3000");
                                    pi.image_url = prefix +
                                            new String(Base64.encode(array.toString().getBytes(), Base64.URL_SAFE |
                                                                                                  Base64.NO_PADDING |
                                                                                                  Base64.NO_WRAP)).trim() +
                                            suffix;
                                } catch (Exception e) {
                                    Log.e("Puffin", "Failed to get image url");
                                }
                            }
                            else if(xpp.getName().equalsIgnoreCase("pubDate")) {
                                try {
                                    pi.date = xpp.nextText();
                                } catch (Exception e) {
                                     Log.e("Puffin", "Failed to get publication date");
                                }
                            }

                        }
                    }
                    else if(eventType == XmlPullParser.END_TAG && xpp.getName().equalsIgnoreCase("item")) {
                        insideItem = false;
                        // Add the puffin!
                        puffinItems.add(pi);
                        pi = new PuffinItem();
                    }
                    try {
                        eventType = xpp.next();
                    } catch (Exception e) {
                        Log.e("Puffin", "Error getting next XML element: " + e.getLocalizedMessage());
                    }
                }
            }
        }
    }
}
