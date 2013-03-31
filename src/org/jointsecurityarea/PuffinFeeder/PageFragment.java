package org.jointsecurityarea.PuffinFeeder;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.TextView;
import com.koushikdutta.urlimageviewhelper.UrlImageViewHelper;

public class PageFragment extends Fragment {

    public static PageFragment newInstance(PuffinItem puffinItem) {

        PageFragment pageFragment = new PageFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", puffinItem.title);
        bundle.putString("link", puffinItem.link);
        bundle.putString("image_url", puffinItem.image_url);
        bundle.putString("date", puffinItem.date);
        pageFragment.setArguments(bundle);
        return pageFragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment, container, false);
        TextView textView = (TextView) view.findViewById(R.id.puffin_title);
        textView.setText(getArguments().getString("title"));
        textView = (TextView) view.findViewById(R.id.puffin_link);
        textView.setText(getArguments().getString("link"));
        WebView imageView = (WebView) view.findViewById(R.id.puffin_image);
        imageView.loadUrl(getArguments().getString("image_url"));
        imageView.getSettings().setBuiltInZoomControls(true);
        imageView.getSettings().setSupportZoom(true);
        imageView.getSettings().setUseWideViewPort(true);
        imageView.getSettings().setLoadWithOverviewMode(true);
        textView = (TextView) view.findViewById(R.id.puffin_date);
        textView.setText(getArguments().getString("date"));

        return view;
    }
}
