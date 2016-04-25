package prj.seesaw;

import narl.itrc.StgBundle;
import javafx.beans.property.ReadOnlyIntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.concurrent.Task;

public class DevMP21xx extends StgBundle {
	
	public DevMP21xx(){
	}
	
	@Override
	public void setup(int idx, String confName) {
		if(dummyDriver.isRunning()==true){
			return;
		}
		new Thread(dummyDriver,"MP2100-driver").start();
	}

	@Override
	public long getPosition(int idx) {
		ReadOnlyIntegerProperty axis = getAxis(idx);
		if(axis==null){
			return -1L;
		}
		return axis.get();
	}

	@Override
	public void setPosition(char typ, int idx, int pulse) {
		if(checkWorking()==true){ return; }
		//relative or absolute position
		type = checkType(typ);
		aidx = idx;
		ppse = pulse;
	}

	@Override
	public void setJogger(char typ, int idx) {
		//The first caller begin to run,then the second caller stop all axis
		if(type=='+' || type=='-'){
			type = '*';
			aidx = -1;
		}else if(type=='*'){
			type = checkTypeJog(typ);
			aidx = idx;
		}
	}
	
	private boolean checkWorking(){
		if(type=='*'){ 
			return false;
		}		
		return true;
	}
	
	@Override
	public void close() {
		dummyDriver.cancel();
	}

	private Task<Void> dummyDriver = new Task<Void>(){
		@Override
		protected Void call() throws Exception {
			
			while(isCancelled()==false){
				try{
					Thread.sleep(1);//just put a delay~~~
				}catch(InterruptedException e){					
				}
				if(type=='*'){					
					continue;
				}				
				SimpleIntegerProperty axis = getAxis(aidx);
				if(axis==null){
					type = '*';//reset it~~~					
					continue;
				}
				int cpse = axis.get();
				switch(type){				
				case 'r'://relative motion
				case 'R':
					if(ppse>0){
						cpse++;
						ppse--;						
					}else if(ppse<0){
						cpse--;
						ppse++;
					}else{
						type = '*';//pulse is zero!!!
					}
					break;
					
				case 'a'://absolute motion
				case 'A':
					if(cpse>ppse){
						cpse--;
					}else if(cpse<ppse){
						cpse++;
					}else{
						type = '*';//we reach the goal
					}
					break;
					
				case '+':
					cpse++;
					break;
				case '-':
					cpse--;
					break;
				}
				setAxis(aidx,cpse);				
			}
			return null;
		}
	};
}



