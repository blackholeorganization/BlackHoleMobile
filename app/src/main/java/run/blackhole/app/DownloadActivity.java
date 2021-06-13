package run.blackhole.app;

import android.os.Bundle;

import com.getcapacitor.BridgeActivity;
import com.getcapacitor.Plugin;

import java.util.ArrayList;

public class DownloadActivity extends BridgeActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        webAssetsDirectory = "public_download";
        super.onCreate(savedInstanceState);
        this.init(savedInstanceState, new ArrayList<Class<? extends Plugin>>() {{
        }});
    }
}
