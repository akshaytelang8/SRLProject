package cop5618;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

public class ColorHistEq {

    //Use these labels to instantiate you timers.  You will need 8 invocations of now()
	static String[] labels = { "getRGB", "convert to HSB", "create brightness map", "probability array",
			"parallel prefix", "equalize pixels", "setRGB" };

	static Timer colorHistEq_serial(BufferedImage image, BufferedImage newImage) {
		Timer times = new Timer(labels);
		/**
		 * IMPLEMENT SERIAL METHOD
		 */
		try{
			ColorModel colorModel = ColorModel.getRGBdefault();
			int w = image.getWidth();
			int h = image.getHeight();
			times.now(); 
			int[] rgbPixelArray = image.getRGB(0, 0, w, h, new int[w*h], 0, w);
			times.now(); 
			Object[] hsbPixelArray = 
					Arrays.stream(rgbPixelArray)
					.mapToObj(pixel ->  (float[])(java.awt.Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), colorModel.getBlue(pixel), null)))
					.toArray();
			times.now(); 
			int numBins = 200;
			Map<Object, Long> histMap = Arrays.stream(hsbPixelArray)
					  .mapToInt(floatVal -> (int)(((float[]) floatVal)[2]*numBins))
					  .boxed()
					  .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
			times.now(); 
			double[] histogramArray = new double[numBins];
			/**
			 * This is done using a for loop
			 * to account for the case when a bin has a count of 0
			 * If we use streams then the bin with a count of 0 would not be accounted for
			 */
			for(int i = 0; i < numBins; i++){
				histogramArray[i] = histMap.get(i) == null?0:histMap.get(i);
			}
			
			//System.out.println("Number of bins :"+numBins+"numbr of elements in hist Array"+histogramArray.length);
			times.now();
			Arrays.parallelPrefix(histogramArray, (x,y) -> x + y);
			times.now();
			int totalCount = hsbPixelArray.length;
			double[] cumulativeProbability = Arrays.stream(histogramArray)
											.map(freq -> freq / totalCount)
											.toArray();
			 // equalizing the brightness value			
			int[] brightPixelArray = Arrays.stream(hsbPixelArray)
										.mapToInt(floatArr -> Color.HSBtoRGB(((float[]) floatArr)[0], ((float[]) floatArr)[1], (float)cumulativeProbability[(int)(((float[]) floatArr)[2]*numBins)]))
										.toArray();
			times.now();
			newImage.setRGB(0, 0, w, h, brightPixelArray, 0, w);
			times.now();
		}catch(Exception e){
			e.printStackTrace();
		}
		return times;
	}

	static Timer colorHistEq_parallel(FJBufferedImage image, FJBufferedImage newImage) {
		Timer times = new Timer(labels);
		/**
		 * IMPLEMENT PARALLEL METHOD
		 */
		try{
			ColorModel colorModel = ColorModel.getRGBdefault();
			int w = image.getWidth();
			int h = image.getHeight();
			times.now();
			int[] rgbPixelArray = image.getRGB(0, 0, w, h, new int[w*h], 0, w);
			times.now();
			Object[] hsbPixelArray = 
					Arrays.stream(rgbPixelArray)
					.parallel()
					.mapToObj(pixel ->  (float[])(java.awt.Color.RGBtoHSB(colorModel.getRed(pixel), colorModel.getGreen(pixel), colorModel.getBlue(pixel), null)))
					.toArray();
			times.now();
			int numBins = 200;
			Map<Object, Long> histMap = Arrays.stream(hsbPixelArray)
					  .parallel()
					  .mapToInt(floatVal -> (int)(((float[]) floatVal)[2]*numBins))
					  .boxed()
					  .collect(Collectors.groupingBy(e -> e, Collectors.counting()));
			times.now();
			double[] histogramArray = new double[numBins];
			/**
			 * This is done using a for loop
			 * to account for the case when a bin has a count of 0
			 * If we use streams then the bin with a count of 0 would not be accounted for
			 */
			for(int i = 0; i < numBins; i++){
				histogramArray[i] = histMap.get(i) == null?0:histMap.get(i);
			}
			
			//System.out.println("Number of bins :"+numBins+"numbr of elements in hist Array"+histogramArray.length);
			times.now();
			Arrays.parallelPrefix(histogramArray, (x,y) -> x + y);
			times.now();
			int totalCount = hsbPixelArray.length;
			double[] cumulativeProbability = Arrays.stream(histogramArray)
											.parallel()
											.map(freq -> freq / totalCount)
											.toArray();
			 // equalizing the brightness value			
			int[] brightPixelArray = Arrays.stream(hsbPixelArray)
										.parallel()
										.mapToInt(floatArr -> Color.HSBtoRGB(((float[]) floatArr)[0], ((float[]) floatArr)[1], (float)cumulativeProbability[(int)(((float[]) floatArr)[2]*numBins)]))
										.toArray();
			times.now();
			newImage.setRGB(0, 0, w, h, brightPixelArray, 0, w);
			times.now();
		}catch(Exception e){
			e.printStackTrace();
		}
		return times;
	}

}
