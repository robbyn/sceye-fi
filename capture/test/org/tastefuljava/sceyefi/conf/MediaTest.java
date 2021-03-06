package org.tastefuljava.sceyefi.conf;

import java.io.File;
import java.io.IOException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import org.tastefuljava.sceyefi.capture.conf.EyeFiCard;
import org.tastefuljava.sceyefi.capture.conf.EyeFiConf;
import org.tastefuljava.sceyefi.capture.conf.Media;

public class MediaTest {
    private Media media;

    @Before
    public void setUp() throws IOException {
        EyeFiConf conf = EyeFiConf.load(getClass().getResource("Settings.xml"));
        EyeFiCard card = conf.getCard("001856417729");
        media = card.getMedia(Media.TYPE_PHOTO);
    }

    @Test
    public void testType() {
        System.out.println("type");
        assertEquals(Media.TYPE_PHOTO, media.getType());
    }

    @Test
    public void testFolder() {
        System.out.println("folder");
        assertEquals(new File("/Users/maurice/Pictures/Eye-Fi"),
                media.getFolder());
    }

    @Test
    public void testAddDate() {
        System.out.println("addDate");
        assertTrue(media.getAddDate());
    }

    @Test
    public void testDateType() {
        System.out.println("dateType");
        assertEquals(1, media.getDateType());
    }

    @Test
    public void testCustomDateFormat() {
        System.out.println("customDateFormat");
        assertEquals("", media.getCustomDateFormat());
    }
}
