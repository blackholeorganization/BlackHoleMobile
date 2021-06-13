package com.getcapacitor;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.BaseInputConnection;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.webkit.WebView;

public class CapacitorWebView extends WebView {
  private BaseInputConnection capInputConnection;
  private boolean isWindowVisible = false;

  public CapacitorWebView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
    CapConfig config = new CapConfig(getContext().getAssets(), null);
    boolean captureInput = config.getBoolean("android.captureInput", false);
    if (captureInput) {
      if (capInputConnection == null) {
        capInputConnection = new BaseInputConnection(this, false);
      }
      return capInputConnection;
    }
    return super.onCreateInputConnection(outAttrs);
  }

  @Override
  protected void onWindowVisibilityChanged(int visibility) {
//    if(isWindowVisible){
    //TODO: temporary, attention battery usage
      super.onWindowVisibilityChanged(View.VISIBLE);
//    }else {
//      super.onWindowVisibilityChanged(visibility);
//    }
  }

//  public void setWindowVisible(boolean windowVisible) {
//    isWindowVisible = windowVisible;
//  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getAction() == KeyEvent.ACTION_MULTIPLE) {
      evaluateJavascript("document.activeElement.value = document.activeElement.value + '" + event.getCharacters() + "';", null);
      return false;
    }
    return super.dispatchKeyEvent(event);
  }
}
