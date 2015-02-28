package ss12.com.lightsout;

import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;

/**
 * Created by Alex on 2/25/2015.
 */

public class SoundEng extends MobileMain {
    public static MediaPlayer mysounds;
    public static void playSoundsFX ()
    {
        // mysounds = new MediaPlayer();
    }

    //sets the context used and using a string the location of the sound
    public static void playSound(Context c, String sound) {
        Uri loc = getMediaLocation(sound);
        mysounds = new MediaPlayer();
        //MediaPlayer.create(c, loc).start();
        mysounds.create(c,loc);
        mysounds.start();
    }

    //sets the context used and takes a Uri instead of string just in case the name is not taking
    public static void playSound(Context c, Uri location) {
        mysounds.create(c,location);
        mysounds.start();
    }
    // Creates a Uri based on know sounds
    public static Uri getMediaLocation(String type) {
        String act = null;
        {
            switch (type) {
                case "punchnoise": {
                    act = "punch";
                }
                break;
                case "counternoise": {
                    act = "startGame";
                }
                case "start": {
                    act = "start";
                }
                case "ding": {
                    act = "ding";
                }
                case "win": {
                    act = "win";
                }
                case "lose": {
                    act = "lose";
                }
                case "tie": {
                    act = "tie";
                }
                case "dopunch": {
                    act = "dopunch";
                }
                case "docounter": {
                    act = "docounter";
                }
                case "cheering": {
                    act = "cheering";
                }
                case "doblock": {
                    act = "doblock";
                }
                default:
                    break;
            }

        }
        String soundLocations = "R.raw." + act;
        Uri soundLocation = Uri.parse(soundLocations);
        return soundLocation;
    }



}


