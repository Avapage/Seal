package com.avapage.video;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import java.io.*;
import java.util.concurrent.Executors;
import kotlin.Unit;

public class MainActivity extends Activity {
    private final java.util.concurrent.ExecutorService worker = Executors.newSingleThreadExecutor();
    private EditText link;
    private TextView status;
    private ProgressBar progress;
    private Spinner format;
    private Button start, update;
    private volatile boolean ready = false;
    private final String processId = "avapage-download";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(13,17,31));
        getWindow().setNavigationBarColor(Color.rgb(13,17,31));
        buildUi();
        acceptIntent(getIntent());
        setBusy(true);
        worker.execute(() -> {
            try {
                YoutubeDL.getInstance().init(getApplicationContext());
                FFmpeg.getInstance().init(getApplicationContext());
                ready = true;
                show("Siap. Tempel tautan video untuk mulai.");
            } catch (Exception e) { show("Gagal menyiapkan mesin: " + message(e)); }
            runOnUiThread(() -> setBusy(false));
        });
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); acceptIntent(intent); }
    private void acceptIntent(Intent intent) {
        if (intent != null && Intent.ACTION_SEND.equals(intent.getAction())) {
            String text = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (text != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("https?://\\S+").matcher(text);
                link.setText(m.find() ? m.group() : text);
            }
        }
    }
    private TextView text(String s, int size, int color) { TextView t = new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color); return t; }
    private void buildUi() {
        int white = Color.rgb(240,243,255), muted = Color.rgb(165,177,199);
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(Color.rgb(13,17,31));
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(32,56,32,36); scroll.addView(root); setContentView(scroll);
        TextView badge = text("AVA  /  VIDEO TOOL", 12, Color.rgb(125,231,204)); root.addView(badge);
        TextView title = text("AvaDownload", 32, white); title.setPadding(0,22,0,8); root.addView(title);
        TextView intro = text("Simpan video dan audio langsung ke perangkat.", 16, muted); intro.setPadding(0,0,0,32); root.addView(intro);
        TextView label = text("TAUTAN VIDEO", 12, muted); root.addView(label);
        link = new EditText(this); link.setSingleLine(false); link.setMinLines(2); link.setTextColor(white); link.setHintTextColor(muted); link.setHint("https://..."); link.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI); root.addView(link);
        LinearLayout row = new LinearLayout(this); row.setGravity(Gravity.END); root.addView(row);
        Button paste = new Button(this); paste.setText("Tempel"); row.addView(paste); paste.setOnClickListener(v -> { android.content.ClipboardManager cb=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE); if(cb.hasPrimaryClip() && cb.getPrimaryClip()!=null) link.setText(cb.getPrimaryClip().getItemAt(0).coerceToText(this)); });
        TextView fLabel = text("FORMAT", 12, muted); fLabel.setPadding(0,20,0,4); root.addView(fLabel);
        format = new Spinner(this); String[] formats={"Video terbaik", "Video maksimal 1080p", "Audio MP3"}; ArrayAdapter<String> adapter=new ArrayAdapter<>(this,android.R.layout.simple_spinner_dropdown_item,formats); format.setAdapter(adapter); root.addView(format);
        start = new Button(this); start.setText("Download"); root.addView(start); start.setOnClickListener(v -> download());
        update = new Button(this); update.setText("Update mesin pengunduh"); root.addView(update); update.setOnClickListener(v -> updateEngine());
        progress = new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal); progress.setMax(100); root.addView(progress);
        status = text("Menyiapkan aplikasi...", 14, muted); status.setTextIsSelectable(true); status.setPadding(0,16,0,0); root.addView(status);
        TextView note = text("Hasil: Downloads / AvaDownload\nBiarkan aplikasi terbuka selama unduhan. Dukungan situs bergantung pada yt-dlp.", 13, muted); note.setPadding(0,36,0,0); root.addView(note);
    }
    private void setBusy(boolean busy) { start.setEnabled(!busy && ready); update.setEnabled(!busy && ready); }
    private void show(String s) { runOnUiThread(() -> status.setText(s)); }
    private String message(Exception e) { return e.getMessage()==null ? e.getClass().getSimpleName() : e.getMessage(); }
    private void updateEngine() {
        if (!ready) return;
        setBusy(true); show("Memeriksa pembaruan...");
        worker.execute(() -> { try { Object result=YoutubeDL.getInstance().updateYoutubeDL(getApplicationContext(), YoutubeDL.UpdateChannel._STABLE); show("Pembaruan: " + result); } catch(Exception e) { show("Update gagal: " + message(e)); } finally { runOnUiThread(() -> setBusy(false)); } });
    }
    private void download() {
        String url=link.getText().toString().trim();
        Uri parsed=Uri.parse(url);
        if (!("https".equalsIgnoreCase(parsed.getScheme()) || "http".equalsIgnoreCase(parsed.getScheme())) || parsed.getHost()==null) { link.setError("Masukkan tautan http atau https yang valid"); return; }
        final int selected=format.getSelectedItemPosition();
        setBusy(true); progress.setProgress(0); show("Menyiapkan unduhan...");
        worker.execute(() -> {
            File dir=new File(getCacheDir(), "job-"+System.currentTimeMillis());
            try {
                if(!dir.mkdirs()) throw new IOException("Tidak bisa menyiapkan penyimpanan sementara");
                YoutubeDLRequest request = new YoutubeDLRequest(url);
                request.addOption("--no-playlist"); request.addOption("--no-mtime");
                request.addOption("-o", new File(dir, "%(title).120B [%(id)s].%(ext)s").getAbsolutePath());
                if(selected==2) { request.addOption("-x"); request.addOption("--audio-format", "mp3"); }
                else if(selected==1) request.addOption("-f", "bestvideo[height<=1080]+bestaudio/best[height<=1080]/best");
                else request.addOption("-f", "bestvideo+bestaudio/best");
                YoutubeDL.getInstance().execute(request, processId, (p, eta, line) -> { runOnUiThread(() -> { progress.setProgress(Math.max(0,Math.min(100,(int)(float)p))); status.setText(line); }); return Unit.INSTANCE; });
                File[] files=dir.listFiles(f -> f.isFile() && !f.getName().endsWith(".part") && !f.getName().endsWith(".ytdl"));
                if(files==null || files.length==0) throw new IOException("Tidak ada file yang dihasilkan");
                for(File file:files) saveToDownloads(file);
                show("Selesai! File disimpan di Downloads/AvaDownload.");
                runOnUiThread(() -> progress.setProgress(100));
            } catch(Exception e) { show("Unduhan gagal: " + message(e) + "\nCoba update mesin atau gunakan tautan lain."); }
            finally { File[] files=dir.listFiles(); if(files!=null) for(File f:files) f.delete(); dir.delete(); runOnUiThread(() -> setBusy(false)); }
        });
    }
    private void saveToDownloads(File file) throws IOException {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Downloads.DISPLAY_NAME,file.getName());
        String ext=android.webkit.MimeTypeMap.getFileExtensionFromUrl(file.getName());
        String mime=android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext.toLowerCase(java.util.Locale.ROOT));
        values.put(MediaStore.Downloads.MIME_TYPE,mime==null?"application/octet-stream":mime);
        values.put(MediaStore.Downloads.RELATIVE_PATH,Environment.DIRECTORY_DOWNLOADS+"/AvaDownload");
        values.put(MediaStore.Downloads.IS_PENDING,1);
        Uri uri=getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,values);
        if(uri==null) throw new IOException("Tidak bisa membuat file di Downloads");
        try (InputStream in=new FileInputStream(file); OutputStream out=getContentResolver().openOutputStream(uri)) {
            if(out==null) throw new IOException("Tidak bisa membuka file tujuan");
            byte[] buf=new byte[65536]; int n; while((n=in.read(buf))!=-1) out.write(buf,0,n);
            ContentValues done=new ContentValues(); done.put(MediaStore.Downloads.IS_PENDING,0); getContentResolver().update(uri,done,null,null);
        } catch (IOException e) { getContentResolver().delete(uri,null,null); throw e; }
    }
}
