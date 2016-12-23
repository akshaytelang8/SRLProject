package cop5618;

import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.IndexColorModel;
import java.awt.image.WritableRaster;
import java.util.Hashtable;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

public class FJBufferedImage extends BufferedImage {
	
   /**Constructors*/
	public FJBufferedImage(int width, int height, int imageType) {
		super(width, height, imageType);
	}

	public FJBufferedImage(int width, int height, int imageType, IndexColorModel cm) {
		super(width, height, imageType, cm);
	}

	public FJBufferedImage(ColorModel cm, WritableRaster raster, boolean isRasterPremultiplied,
			Hashtable<?, ?> properties) {
		super(cm, raster, isRasterPremultiplied, properties);
	}
	

	/**
	 * Creates a new FJBufferedImage with the same fields as source.
	 * @param source
	 * @return
	 */
	public static FJBufferedImage BufferedImageToFJBufferedImage(BufferedImage source){
	       Hashtable<String,Object> properties=null; 
	       String[] propertyNames = source.getPropertyNames();
	       if (propertyNames != null) {
	    	   properties = new Hashtable<String,Object>();
	    	   for (String name: propertyNames){properties.put(name, source.getProperty(name));}
	    	   }
	 	   return new FJBufferedImage(source.getColorModel(), source.getRaster(), source.isAlphaPremultiplied(), properties);		
	}
	
	@Override
	public void setRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){

		/****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
		int cores = Runtime.getRuntime().availableProcessors();
		int dim = (int) Math.ceil((double) h / cores);
		//int dim = (int) Math.ceil((double) h / 2);

		ForkJoinPool pool = new ForkJoinPool(8);
	    
	    class ParallelRGBSET extends RecursiveAction{
			private static final long serialVersionUID = 1L;

	    	int xStart;
	    	int yStart;
	    	int w;
	    	int h; 
	    	int[] rgbArray; 
	    	int offset; 
	    	int scansize;
	    	ColorModel colorModel;
	    	WritableRaster raster;
	    	int dim;
	    	int proc;
	    	
	    	public ParallelRGBSET(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize, int dim, ColorModel cm, WritableRaster raster, int proc) {
	    		this.xStart = xStart;
	    		this.yStart = yStart;
	    		this.w = w;
	    		this.h = h;
	    		this.rgbArray = rgbArray;
	    		this.offset = offset;
	    		this.scansize = scansize;
	    		this.dim = dim;
	    		this.colorModel = cm;
	    		this.raster = raster;
	    		this.proc = proc;
	    	}
	    	
	    	@Override
			protected void compute() {
	    	/**
	    	 * If processors are less than 2 
	    	 * then the task would proceed sequentially
	    	 */
				if(proc < 2){
					computeDirectly();
				}			
				
				//if(this.h > 747)
				if(this.h > dim)
				{	
					createSubtasks();
					
				}else{
					//start worker here
					int yoff  = offset;
				    int off;
				    Object pixel = null;

				    for (int y = yStart; y < yStart+h; y++, yoff+=this.scansize) {
				       off = yoff;
				       for (int x = this.xStart; x < this.xStart+w; x++) {
				         pixel = colorModel.getDataElements(rgbArray[off++], pixel);
				         raster.setDataElements(x, y, pixel);
				        }
				     }
				}
			}
	    	
	    	private void createSubtasks() {
	            
	            //int[] part1 = new int[w*h/2];
	            //int[] part2 = new int[w*h/2];
	    		int partitionIndex = h % 2 == 0? h / 2: (int) Math.floor((double) h / 2);
	    		int endIndex = h % 2 == 0? h / 2: (int) Math.ceil((double) h / 2);
	    		
	    		/*System.out.println("[Set] Start Index is " + partitionIndex);
	    		System.out.println("[Set] End Index is "+ endIndex);
	    		System.out.println("[Set] Length of array is " + rgbArray.length);
	    		System.out.println("[Set] Dimension is "+ dim);*/
	    		
	    		int[] part1 = new int[w*partitionIndex];
	            int[] part2 = new int[w*endIndex];
	            //Create two subarrays and pass them to actions
	            System.arraycopy(rgbArray, 0, part1, 0, part1.length);
	            System.arraycopy(rgbArray, part1.length, part2, 0, part2.length);
	            
	            //ParallelRGBSET subtask1 = new ParallelRGBSET(xStart, yStart, w, h/2, part1, offset, scansize, dim, colorModel, raster,Runtime.getRuntime().availableProcessors());
	            ///ParallelRGBSET subtask2 = new ParallelRGBSET(xStart, yStart+h/2, w, h/2, part2, offset, scansize, dim, colorModel, raster,Runtime.getRuntime().availableProcessors());

	            ParallelRGBSET subtask1 = new ParallelRGBSET(xStart, yStart, w, partitionIndex, part1, offset, scansize, dim, colorModel, raster,Runtime.getRuntime().availableProcessors());
	            ParallelRGBSET subtask2 = new ParallelRGBSET(xStart, yStart+partitionIndex, w, endIndex, part2, offset, scansize, dim, colorModel, raster,Runtime.getRuntime().availableProcessors());
	            
	            invokeAll(subtask1,subtask2);
	        }
	    	
	    	private void computeDirectly(){int yoff  = offset;
		    int off;
		    Object pixel = null;

		    for (int y = yStart; y < yStart+h; y++, yoff+=scansize) {
		       off = yoff;
		       for (int x = xStart; x < xStart+w; x++) {
		         pixel = colorModel.getDataElements(rgbArray[off++], pixel);
		         raster.setDataElements(x, y, pixel);
		        }
		     }}
	    }
	    pool.invoke(new ParallelRGBSET(xStart, yStart, w, h, rgbArray, offset, scansize, dim, this.getColorModel(), this.getRaster(), cores));

	}
	

	@Override
	public int[] getRGB(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize){
       
	/****IMPLEMENT THIS METHOD USING PARALLEL DIVIDE AND CONQUER*****/
		//Starting parallel implementation
		int cores = Runtime.getRuntime().availableProcessors();
		int dim = (int) Math.ceil((double) h / 8);
		
	    ForkJoinPool pool = new ForkJoinPool(cores);
	    class ParallelRGBGET extends RecursiveTask<int[]>{

			private static final long serialVersionUID = 1L;
			int xStart;
			int yStart;
			int w;
			int h;
			ColorModel colorModel;
			WritableRaster raster;
			
			public ParallelRGBGET(int xStart, int yStart, int w, int h, int[] rgbArray, int offset, int scansize, int dim, ColorModel cm, WritableRaster raster) {
				this.xStart = xStart;
				this.yStart = yStart;
				this.w = w;
				this.h = h;
				this.rgbArray = rgbArray;
				this.offset = offset;
				this.scansize = scansize;
				this.dim = dim;
				this.colorModel = cm;
				this.raster = raster;
			}

			int[] rgbArray;
			int offset;
			int scansize;
			int dim;

			@Override
			protected int[] compute() {

				//if(this.h > 747)
				if(this.h > dim)
				{
		            createSubtasks();	
		            return rgbArray;
				
				}else{
					Object data;
					int off;
					int nbands = raster.getNumBands();
					int dataType = raster.getDataBuffer().getDataType();
					switch (dataType) {
					case DataBuffer.TYPE_BYTE:
					data = new byte[nbands];
					break;
					case DataBuffer.TYPE_USHORT:
					data = new short[nbands];
					break;
					case DataBuffer.TYPE_INT:
					data = new int[nbands];
					break;
					case DataBuffer.TYPE_FLOAT:
					data = new float[nbands];
					break;
					case DataBuffer.TYPE_DOUBLE:
					data = new double[nbands];
					break;
					default:
					throw new IllegalArgumentException("Unknown data buffer type: "+
					dataType);
					}

					if (rgbArray == null) {
					rgbArray = new int[offset+h*scansize];
					}

					for (int y = yStart; y < yStart+h; y++, offset+=scansize) {
					off = offset;
					for (int x = xStart; x < xStart+w; x++) {
						rgbArray[off++] = colorModel.getRGB(raster.getDataElements(x,y,data));
					}
					}
					
					return rgbArray;
				}
			}
			
			private void createSubtasks() {
		        //int[] part1 = new int[w*(int) Math.floor((double) h / 2)];
		        //int[] part2 = new int[w*(int) Math.floor((double) h / 2)];
				int partitionIndex = h % 2 == 0? h / 2: (int) Math.floor((double) h / 2);
	    		int endIndex = h % 2 == 0? h / 2: (int) Math.ceil((double) h / 2);
	    		/*
	    		System.out.println("Start Index is " + partitionIndex);
	    		System.out.println("End Index is "+ endIndex);
	    		System.out.println("Length of array is " + rgbArray.length);
	    		System.out.println("Dimension is "+ dim);*/
	    		
	    		int[] part1 = new int[w*partitionIndex];
	            int[] part2 = new int[w*endIndex];
	            
		        //ParallelRGBGET subtask1 = new ParallelRGBGET(xStart, yStart, w, h/2, part1, offset, scansize, dim, colorModel, raster);
		        //ParallelRGBGET subtask2 = new ParallelRGBGET(xStart, yStart+h/2, w, h/2, part2, offset, scansize, dim, colorModel, raster);
		        
		        ParallelRGBGET subtask1 = new ParallelRGBGET(xStart, yStart, w, partitionIndex, part1, offset, scansize, dim, colorModel, raster);
		        ParallelRGBGET subtask2 = new ParallelRGBGET(xStart, yStart+partitionIndex, w, endIndex, part2, offset, scansize, dim, colorModel, raster);
		        
		        invokeAll(subtask1,subtask2);
		        System.arraycopy(part1, 0, rgbArray, 0, part1.length);
		        System.arraycopy(part2, 0, rgbArray, part1.length, part2.length);
		    }
	}
			int[] rgbArrayOut = pool.invoke(new ParallelRGBGET(xStart, yStart, w, h, rgbArray, offset, scansize, dim, this.getColorModel(), this.getRaster()));
			return rgbArrayOut;
	}
}

	
