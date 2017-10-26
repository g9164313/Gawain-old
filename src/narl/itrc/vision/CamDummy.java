package narl.itrc.vision;

/**
 * Virtual image grabber, data can be image files or memory.<p>
 * !! Create a memory pool to keep image data. !!<p>
 * @author qq
 *
 */
public class CamDummy extends CamBundle {

	public CamDummy(String... names){
		
	}
	
	public CamDummy(int width, int height){
		
	}	
	//-------------------------------//
	
	private native void implSetup(CamBundle cam);
	private native void implFetch(CamBundle cam);
	private native void implClose(CamBundle cam);
	
	/**
	 * Memory pool pointer.
	 */
	private long poolPtr = 0L;
	
	/**
	 * Memory pool size.
	 */
	private long poolLen = 0L;
	
	
	@Override
	public void setup() {
		implSetup(this);
	}

	@Override
	public void fetch() {
		implFetch(this);
	}

	@Override
	public void close() {
		implClose(this);
	}
}
