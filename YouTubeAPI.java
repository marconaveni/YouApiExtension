package com.marco.YouTubeAPI;

import com.google.appinventor.components.annotations.DesignerComponent;
import com.google.appinventor.components.annotations.DesignerProperty;
import com.google.appinventor.components.annotations.PropertyCategory;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.appinventor.components.annotations.SimpleObject;
import com.google.appinventor.components.annotations.SimpleProperty;
import com.google.appinventor.components.annotations.UsesPermissions;
import com.google.appinventor.components.common.ComponentCategory;
import com.google.appinventor.components.common.PropertyTypeConstants;
import com.google.appinventor.components.runtime.AndroidNonvisibleComponent;
import com.google.appinventor.components.runtime.ComponentContainer;
import com.google.appinventor.components.runtime.EventDispatcher;
import com.google.appinventor.components.runtime.util.JsonUtil;
import com.google.appinventor.components.annotations.UsesLibraries;
import com.google.appinventor.components.runtime.util.YailList;
import com.google.appinventor.components.runtime.util.ElementsUtil;

import com.google.gson.Gson;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import android.os.StrictMode;

@DesignerComponent(version = 1, description = "Extension created by<br>"
                    + "Marco Naveni <br>", 
                   category = ComponentCategory.EXTENSION,
                   nonVisible = true, 
                   iconName = "https://s.ytimg.com/yts/img/favicon-vfl8qSV2F.ico")

@SimpleObject(external = true)
@UsesPermissions(permissionNames = "android.permission.INTERNET")
@UsesLibraries(libraries = "gson-2.1.jar")
public final class YouTubeAPI extends AndroidNonvisibleComponent {

    private static final String API_URL_PLAYLIST = "https://www.googleapis.com/youtube/v3/playlistItems?";
    private static final String API_URL_VIDEO = "https://www.googleapis.com/youtube/v3/videos?";
    public static String key = ""; // YouTubeAPI KEY
    private static final String MAXRESULT = "50";
    private static final String PART = "snippet";


    public static List<String> thumbnails = new ArrayList<String>();
    public static List<String> titles = new ArrayList<String>();
    public static List<String> videos = new ArrayList<String>();
    public static List<String> descriptions = new ArrayList<String>();

    /**
     * Creates a new component
     */
    public YouTubeAPI(ComponentContainer container) {
        super(container.$form());
        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }
    }

    @SimpleFunction(description = "get videos playlist ")
    public void GetVideosList(String urlPlaylist) {
        try {
            
            thumbnails.clear();
            titles.clear();
            videos.clear();
            descriptions.clear();

            YailList thumbnailYa;
            YailList titleYa;
            YailList videoIdYa;
            YailList descriptionYa;

            String videoId = "";
            RespostaYoutube respostaYoutube = obterDadosPlayList(urlPlaylist, null);
            if (respostaYoutube.pageInfo.totalResults < 500) {

                int results = respostaYoutube.items.size();

                setListVideos(results,respostaYoutube);
                if (respostaYoutube.nextPageToken != null) {
                    do {
                        respostaYoutube = obterDadosPlayList(urlPlaylist, respostaYoutube.nextPageToken);
                        results = respostaYoutube.items.size();
                        setListVideos(results,respostaYoutube);

                    } while (respostaYoutube.nextPageToken != null);

                } // end if
            
            } // end if
            thumbnailYa = YailList.makeList(thumbnails);
            titleYa = YailList.makeList(titles);
            videoIdYa = YailList.makeList(videos);
            descriptionYa = YailList.makeList(descriptions);
            GotVideosPlayList(thumbnailYa, titleYa, videoIdYa, descriptionYa);


        } catch (Exception e) {

        }

    }

    private void setListVideos(int results ,RespostaYoutube respostaYoutube){
        for (int i = 0; i < results; i++) {
                    
            thumbnails.add(respostaYoutube.items.get(i).snippet.thumbnails.medium.url);
            titles.add(respostaYoutube.items.get(i).snippet.title);
            videos.add(respostaYoutube.items.get(i).snippet.resourceId.videoId);
            descriptions.add(respostaYoutube.items.get(i).snippet.description);
        }
    }


    @SimpleFunction(description = "get video informations")
    public void GetVideo(String urlVideo) {
        RespostaYoutube respostaYoutube = obterDadosVideo(urlVideo);
        GotVideo(respostaYoutube.items.get(0).snippet.thumbnails.medium.url, 
                 respostaYoutube.items.get(0).snippet.title,
                 respostaYoutube.items.get(0).id,
                 respostaYoutube.items.get(0).snippet.description);
    }


    @SimpleEvent(description = "event GotVideosPlayList")
    public void GotVideosPlayList(YailList thumbnail, YailList title, YailList videoId, YailList description) {
        EventDispatcher.dispatchEvent(this, "GotVideosPlayList", thumbnail, title, videoId, description);
    }


    @SimpleEvent(description = "event GotVideo")
    public void GotVideo(String thumbnail, String title, String videoId, String description) {
        EventDispatcher.dispatchEvent(this, "GotVideo", thumbnail, title, videoId, description);
    }

    @SimpleProperty(category = PropertyCategory.APPEARANCE, description = "Google api key v3")
    public String APIkey() {
        return key;
    }

    @DesignerProperty(editorType = PropertyTypeConstants.PROPERTY_TYPE_STRING, defaultValue = "")
    @SimpleProperty
    public void APIkey(String newkey) {
        key = newkey;
    }

    public static RespostaYoutube obterDadosPlayList(String URL, String page) {

        String pageToken = page != null ? "pageToken=" + page + "&" : ""; // adiciona "pageToken=" ou ""

        Gson gson = new Gson();
        String retornoJson = null;
        try {
            // Faz o split para obter o ID do video
            String ID = URL.split("list=")[1];
            retornoJson = lerUrl(API_URL_PLAYLIST + "playlistId=" + ID + "&" + "key=" + key + "&" + pageToken
                    + "maxResults=" + MAXRESULT + "&" + "part=" + PART);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return gson.fromJson(retornoJson, RespostaYoutube.class);
    }

    public static RespostaYoutube obterDadosVideo(String URL) {

        Gson gson = new Gson();
        String retornoJson = null;
        try {
            String ID = URL;
            retornoJson = lerUrl(API_URL_VIDEO + "id=" + ID + "&" + "key=" + key + "&" + "part=" + PART);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        return gson.fromJson(retornoJson, RespostaYoutube.class);
    }

    private static String lerUrl(String urlString) {
        BufferedReader leitor = null;
        try {
            URL url = new URL(urlString);
            leitor = new BufferedReader(new InputStreamReader(url.openStream()));
            StringBuffer buffer = new StringBuffer();
            int read;
            char[] chars = new char[1024];
            while ((read = leitor.read(chars)) != -1)
                buffer.append(chars, 0, read);

            return buffer.toString();
        } catch (Exception e) {
            return e.toString();
        } finally {
            try {
                if (leitor != null)
                    leitor.close();
            } catch (Exception e) {

            }
        }

    }

    public class RespostaYoutube {
        public List<Items> items = new ArrayList<Items>();
        public PageInfo pageInfo;
        public String nextPageToken;
    }

    public class PageInfo {
        public int resultsPerPage;
        public int totalResults;
    }

    public class Items {
        public Snippet snippet;
        public String id;
    }

    public class Snippet {
        public String title;
        public String description;
        public Thumbnails thumbnails;
        public List<String> tags = new ArrayList<String>();
        public ResourceId resourceId;
    }

    public class ResourceId {
        public String videoId;
    }

    public class Thumbnails {
        Medium medium;
        High high;
    }

    public class Medium {
        String url;
        long width;
        long height;
    }

    public class High {
        String url;
        long width;
        long height;
    }

}
