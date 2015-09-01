package uk.ac.ebi.arrayexpress.components.thumbnails;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.util.ImageIOUtil;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

/**
 * Created by awais on 01/09/2015.
 */
public class TXTThumbnail implements IThumbnail{

    private Color background = Color.WHITE;
    private Font font = new Font("sans-serif", Font.PLAIN, 4);
    private static String [] supportedTypes= {"txt","csv"};

    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }
    @Override
    public void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException{
        try(FileInputStream source = new FileInputStream(sourceFilePath) )
        {

            AttributedString text =  new AttributedString(IOUtils.toString(source));
            BufferedImage image = new BufferedImage(200,200, BufferedImage.TYPE_INT_RGB);
            Graphics2D g = image.createGraphics();
            g.setColor(background);
            g.fillRect(0, 0, 200, 200);
            g.setColor(Color.BLACK);
            g.setFont(font);
            AttributedCharacterIterator paragraph = text.getIterator();
            int paragraphStart = paragraph.getBeginIndex();
            int paragraphEnd = paragraph.getEndIndex();
            FontRenderContext frc = g.getFontRenderContext();
            LineBreakMeasurer lineMeasurer = new LineBreakMeasurer(paragraph, frc);
            float breakWidth = 200;
            float drawPosY = 0;
            lineMeasurer.setPosition(paragraphStart);
            while (lineMeasurer.getPosition() < paragraphEnd) {
                TextLayout layout = lineMeasurer.nextLayout(breakWidth);
                float drawPosX = layout.isLeftToRight() ? 0 : breakWidth - layout.getAdvance();
                drawPosY += layout.getAscent();
                layout.draw(g, drawPosX, drawPosY);
                drawPosY += layout.getDescent() + layout.getLeading();
            }

            ImageIOUtil.writeImage(image, thumbnailFile.getAbsolutePath(), 96);
        }

    }
}
