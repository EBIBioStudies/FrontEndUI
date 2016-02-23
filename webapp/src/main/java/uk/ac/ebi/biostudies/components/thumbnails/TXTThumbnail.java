/*
 * Copyright 2009-2016 European Molecular Biology Laboratory
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package uk.ac.ebi.biostudies.components.thumbnails;

import org.apache.pdfbox.util.ImageIOUtil;

import java.awt.*;
import java.awt.font.FontRenderContext;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.AttributedCharacterIterator;
import java.text.AttributedString;

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
            byte[] data      = new byte[512]; // get only the first 0.5K
            int bytesRead = source.read(data);
            AttributedString text =  new AttributedString(new String(data));
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
