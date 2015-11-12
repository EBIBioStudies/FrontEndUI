/*
 * Copyright 2009-2015 European Molecular Biology Laboratory
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

package uk.ac.ebi.arrayexpress.components.thumbnails;

import com.twelvemonkeys.image.ResampleOp;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.io.File;
import java.io.IOException;

public class ImageThumbnail implements IThumbnail{

    String[] supportedTypes = {"bmp","jpg","wbmp","jpeg","png","gif","tif","tiff","pdf","docx","txt","csv","html"};
    @Override
    public String[] getSupportedTypes() {
        return supportedTypes;
    }

    @Override
    public void generateThumbnail(String sourceFilePath, File thumbnailFile) throws IOException{
        File sourceFile = new File(sourceFilePath);
        BufferedImage input = ImageIO.read(sourceFile);
        float height = (float) input.getHeight(), width = (float) input.getWidth();
        if (width > 200 || height>200) {
            float inverseAspectRatio = height / width;
            BufferedImageOp resampler = new ResampleOp(200, Math.round(inverseAspectRatio * 200), ResampleOp.FILTER_LANCZOS);
            BufferedImage output = resampler.filter(input, null);
            if (!ImageIO.write(output, "png", thumbnailFile)) {
                throw new IOException("Cannot write thumbnail");
            }
        } else {
            ImageIO.write(input, "png", thumbnailFile);
        }
    }
}
