package ru.dublgis.offscreenview;

import java.lang.Thread;
import java.util.Set;
import java.util.List;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.TreeSet;
import java.util.Locale;
import java.util.List;
import java.util.LinkedList;
import java.io.File;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.ConfigurationInfo;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.method.MetaKeyKeyListener;
import android.text.InputType;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View;
import android.view.ViewParent;
import android.view.KeyEvent;
import android.view.KeyCharacterMap;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.graphics.Canvas;
import org.qt.core.QtApplicationBase;
import ru.dublgis.offscreenview.OffscreenViewHelper;

class OffscreenWebView extends OffscreenView
{
    MyWebView webview_ = null;

    class MyWebView extends WebView
    {
        // TODO: pauseTimers/ resumeTimers ()
        // http://developer.android.com/reference/android/webkit/WebView.html
        //  public void setInitialScale (int scaleInPercent) (0 = default)

        int width_ = 0, height_ = 0;
        MyWebViewClient webviewclient_ = null;
        boolean invalidated_ = true;

        private class MyWebViewClient extends WebViewClient
        {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url)
            {
                // This should always be done for Chrome to avoid opening links in external browser.
                view.loadUrl(url);
                return true;
            }

            @Override
            public void onPageFinished(WebView view, String url)
            {
                Log.i(TAG, "MyWebView.MyWebViewClient.onPageFinished");
                if (helper_ != null)
                {
                     nativeUpdate(helper_.getNativePtr());
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onLoadResource(WebView view, String url)
            {
                /*(if (url.equals("http://redirectexample.com"))
                {
                ...
                }
                else
                {
                    super.onLoadResource(view, url);
                }*/
                super.onLoadResource(view, url);
            }
        }

        public MyWebView(Context context, int width, int height)
        {
            super(context);
            Log.i(TAG, "MyWebView "+width+"x"+height);
            webviewclient_ = new MyWebViewClient();
            setWebViewClient(webviewclient_);
            //setVerticalScrollBarEnabled(true);
            /*onAttachedToWindow();
            onSizeChanged(width, height, 0, 0);
            onVisibilityChanged(this, View.VISIBLE);
            onLayout(true, 0, 0, width, height);*/
            resizeOffscreenView(width, height);
        }

        // NB: caller should call the invalidation
        public void resizeOffscreenView(int width, int height)
        {
            onSizeChanged(width, height, width_, height_);
            width_ = width;
            height_ = height;
            setLeft(0);
            setRight(width-1);
            setTop(0);
            setBottom(height-1);
        }

        public void onDrawPublic(Canvas canvas)
        {

            Log.i(TAG, "MyWebView.onDrawPublic "+getWidth()+"x"+getHeight());
            canvas.drawARGB (255, 255, 255, 255);

            // Take View scroll into account. (It converts touch coordinates by itself,
            // but it doesn't draw scrolled).
            canvas.translate(-getScrollX(), -getScrollY());

            super.onDraw(canvas);

            /*
            Paint paint = new Paint();
            paint.setColor(Color.BLUE);
            canvas.drawLine(0, 0, getWidth()-1, getHeight()-1, paint);
            canvas.drawLine(getWidth()-1, 0, 0, getHeight()-1, paint);
            canvas.drawLine(0, 0, getWidth()-1, 0, paint);
            canvas.drawLine(0, getHeight()-1, getWidth()-1, getHeight()-1, paint);
            canvas.drawLine(0, 0, 0, getHeight()-1, paint);
            canvas.drawLine(getWidth()-1, 0, getWidth()-1, getHeight()-1, paint);
            // CheckBitmap(); if (bitmap_ != null){ canvas.drawBitmap(bitmap_, 200, 2000, paint); }
            */
        }

        public void invalidateTexture()
        {
            invalidated_ = true;
            // A little dance with a tambourine to filter out subsequent invalidations happened before a single paint
            new Handler().post(new Runnable(){
                @Override
                public void run()
                {
                    Log.i(TAG, "invalidateTexture: processing with invalidated_="+invalidated_);
                    if (invalidated_)
                    {
                        invalidated_ = false;
                        doDrawViewOnTexture();
                    }
                }
            });
        }

        // Old WebKit updating
        @Override
        public void invalidate(Rect dirty)
        {
            Log.i(TAG, "MyWebView.invalidate(Rect dirty)");
            super.invalidate(dirty);
            invalidateTexture();
        }

        // Old WebKit updating
        @Override
        public void invalidate(int l, int t, int r, int b)
        {
            Log.i(TAG, "MyWebView.invalidate(int l, int t, int r, int b) "+l+", "+t+", "+r+", "+b);
            super.invalidate(l, t, r, b);
            invalidateTexture();
        }

        // Old WebKit updating
        @Override
        public void invalidate()
        {
            Log.i(TAG, "MyWebView.invalidate()");
            super.invalidate();
            invalidateTexture();
        }

        /*// ????
        @Override
        public ViewParent invalidateChildInParent(int[] location, Rect r)
        {
            Log.i(TAG, "MyWebView.invalidateChildInParent(int[] location, Rect r)");
            invalidateTexture();
            return super.invalidateChildInParent(location, r);
        }*/

        // Chromium updating
        @Override
        public void requestLayout()
        {
            Log.i(TAG, "MyWebView.requestLayout()");
            if (helper_ != null)
            {
               final Activity context = getContextStatic();
               context.runOnUiThread(new Runnable()
               {
                   @Override
                   public void run()
                   {
                       onLayout(true, 0, 0, width_, height_);
                       invalidateTexture();
                   }
               });
            }
            super.requestLayout();
        }

        /*
        // ????
        @Override
        public void invalidateDrawable(Drawable drawable)
        {
            Log.i(TAG, "MyWebView.invalidateDrawable()");
            invalidateTexture();
        }

        // ????
        @Override
        public void scheduleDrawable(Drawable who, Runnable what, long when)
        {
            Log.i(TAG, "MyWebView.scheduleDrawable()");
            invalidateTexture();
        }

        // ????
        @Override
        public void childDrawableStateChanged(View child)
        {
            Log.i(TAG, "MyWebView.childDrawableStateChanged()");
            invalidateTexture();
            super.childDrawableStateChanged(child);
        }
        */

        //  public boolean isDirty () 
    }

    OffscreenWebView(final String objectname, final long nativeptr, final int gltextureid, final int width, final int height)
    {
        super();
        final Activity context = getContextStatic();
        Log.i(TAG, "OffscreenWebView(name=\""+objectname+"\", texture="+gltextureid+")");
        context.runOnUiThread(new Runnable()
        {
            @Override
            public void run()
            {
                 Log.i(TAG, "OffscreenWebView(name=\""+objectname+"\", texture="+gltextureid+") RUNNABLE");
                 // int save_req_orientation = context.getRequestedOrientation();
                 // Log.i(TAG, "SGEXP orientation was: "+save_req_orientation);
                 webview_ = new MyWebView(context, width, height);
                 helper_ = new OffscreenViewHelper(nativeptr, objectname, webview_, gltextureid, width, height);
                 webview_.getSettings().setJavaScriptEnabled(true);
                 webview_.loadUrl("http://www.android.com/intl/en/about/");
                 //webview_.loadUrl("http://www.youtube.com/watch?v=D36JUfE1oYk");
                 //webview_.loadUrl("http://beta.2gis.ru/");
                 // webview_.loadUrl("http://google.com/");
                 //webview_.loadUrl("http://beta.2gis.ru/novosibirsk/booklet/7?utm_source=products&utm_medium=mobile");
                 // TODO !!! Walkaround !!! Adding WebView disables automatic orientation changes on some devices (with Lite plug-in),
                 // have to figure out why.
                 context.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                 //webview_.loadData("<html><body style=\"background-color: green;\"><h1>Teach Me To Web</h1></body></html>", "text/html", null);
            }
        });
    }

    @Override
    public View getView()
    {
        return webview_;
    }

    public void callViewPaintMethod(Canvas canvas)
    {
        if (webview_ != null)
        {
            webview_.onDrawPublic(canvas);
        }
    }

    public void doInvalidateOffscreenView()
    {
        if (webview_ != null)
        {
            webview_.invalidateTexture();
        }
    }

    public void doResizeOffscreenView(final int width, final int height)
    {
        if (webview_ != null)
        {
            webview_.resizeOffscreenView(width, height);
        }
    }


 
 /*Event processing onKeyDown(int, KeyEvent) Called when a new hardware key event occurs.
onKeyUp(int, KeyEvent) Called when a hardware key up event occurs.
onTrackballEvent(MotionEvent) Called when a trackball motion event occurs.
onTouchEvent(MotionEvent) Called when a touch screen motion event occurs. */

    private native void nativeUpdate(long nativeptr);

    @Override
    public void doNativeUpdate(long nativeptr)
    {
        nativeUpdate(nativeptr);
    }
}

