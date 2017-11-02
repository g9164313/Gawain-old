package narl.itrc.vision;

import java.util.ArrayList;

import narl.itrc.Misc;

/**
 * Virtual image grabber, data can be image files or memory.<p>
 * !! Create a memory pool to keep image data. !!<p>
 * @author qq
 *
 */
public class CamDummy extends CamBundle {

	public CamDummy(){
	}
	
	public CamDummy(String... fileNames){
		addImageFile(fileNames);
		setup();
	}
	
	public CamDummy(int width, int height){
		addImage(width,height);
		setup();
	}
	//-------------------------------//
		
	private class PoolChunk {
		@SuppressWarnings("unused")
		public long ptr,len;
		public int[] geom = { 0, 0, 0};//width, height, OpenCV image type
		public PoolChunk(
			long pointer, long length,
			int width, int height,
			int type
		){
			ptr = pointer;
			len = length;
			geom[0] = width;
			geom[1] = height;
			geom[2] = type;
		}
	};
	
	private int cur = 0;
	private ArrayList<PoolChunk> lst = new ArrayList<PoolChunk>();
	
	/**
	 * This will be called by native code, after loading a image data.<p>
	 */
	private void pushChunkCallback(
		long ptr, long len, 
		int width, int height, 
		int cvType
	){ 
		lst.add(new PoolChunk(ptr,len, width, height, cvType));
	}
	
	private long getAligmentLength(int width, int height, int cvType){
		final long chunk = 1024;
		long len = width * height * CvType.ELEM_SIZE(cvType);
		long rem = len % chunk;
		if(rem!=0L){
			len = len + (chunk-rem);
		}
		return len;
	}
	//-------------------------------//
	
	/**
	 * create a dummy image from memory pool.Content isn't assigned.<p>
	 * The default format is CV_8UC3.<p>
	 * @param width - 
	 * @param height
	 * @return
	 */
	public CamDummy addImage(int width, int height, int type){
		long len = getAligmentLength(width,height,type);
		long ptr = Misc.realloc(0, len);
		pushChunkCallback(
			ptr,len,
			width,height,
			type
		);
		cur = 0;//reset index~~~
		return this;
	}
	
	public CamDummy addImage(int width, int height){
		return addImage(width,height,CvType.CV_8UC3);		
	}
	
	private native void loadImageFile(String name, int type);
	
	public CamDummy addImageFile(String... fileNames){
		for(String name:fileNames){
			loadImageFile(name,CvType.CV_8UC3);
		}
		return this;
	}	
	//-------------------------------//
	
	private native void implFetch(CamBundle cam);	
	
	@Override
	public void setup() {
		fetch();		
	}

	@Override
	public void fetch() {
		if(lst.size()==0){
			return;
		}
		PoolChunk chk = lst.get(cur);
		ptrCntx  = chk.ptr;
		bufSizeW = chk.geom[0];
		bufSizeH = chk.geom[1];
		bufCvFmt = chk.geom[2];
		implFetch(this);
		cur++;
		if(cur>=lst.size()){
			cur = 0;
		}
	}

	@Override
	public void close() {
		cur = 0;
		for(PoolChunk chk:lst){
			Misc.free(chk.ptr);
		}
		lst.clear();
	}
}
