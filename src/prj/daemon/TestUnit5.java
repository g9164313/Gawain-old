package prj.daemon;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.math3.random.RandomDataGenerator;
import org.apache.commons.math3.util.CombinatoricsUtils;

import com.jfoenix.controls.JFXButton;
import com.sun.glass.ui.Application;

import javafx.beans.property.StringProperty;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import narl.itrc.Misc;
import narl.itrc.PanBase;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.params.ScanParams;
import redis.clients.jedis.resps.ScanResult;

public class TestUnit5 extends PanBase {

	public TestUnit5(final Stage stg) {
		super(stg);
		
		AtomicInteger[] count = (AtomicInteger[])Misc.deserializeFile("ggyy.obj");
		String[] hist = new String[30];
		for(int i=0; i<hist.length; i++) {
			int v = count[i].get();
			hist[i] = String.valueOf(v);
		}
				
		/*JedisPool pool = new JedisPool("localhost", 6379);
		Jedis jedis = pool.getResource();
		jedis.rpush("mcmc_hist", hist);
		jedis.close();
		pool.close();
		System.out.println();*/
		
		/*JedisPool pool = new JedisPool("localhost", 6379);
		Jedis jedis = pool.getResource();
		ScanParams scanParams = new ScanParams();
		scanParams.count(100);
		ScanResult<Map.Entry<String, String>> scanResult;
		scanResult = jedis.hscan("arg4", String.valueOf(0),scanParams);
		System.out.print("");
		pool.close();*/
		
		/*final RandomDataGenerator rnd = new RandomDataGenerator();
		gen_sample(null,rnd);
		gen_sample(null,rnd);
		gen_sample(null,rnd);
		gen_sample(null,rnd);*/
		
		//final long choose = CombinatoricsUtils.binomialCoefficient(tkn_size, pck_size);
		//System.out.print(choose);
	}
	
	StringProperty msg;
	
	final int tkn_size = 49;
	final int pck_size = 7;
	
	private void init_space() {
		final long t1 = System.currentTimeMillis();
		
		final JedisPool pool = new JedisPool("localhost", 6379);
		
		final Pipeline pipe = pool.getResource().pipelined();
		
		Iterator<int[]> iter = CombinatoricsUtils.combinationsIterator(tkn_size, pck_size);
		int count = 1;
		final byte[] zero = val2buf(0);
		while(iter.hasNext()) {
			final byte[] name = pick2name(iter.next());
			final byte[] indx = val2buf(count);
			pipe.hsetnx("odds".getBytes(), name, zero);
			//pipe.hsetnx("mcmc".getBytes(), name, zero);
			//pipe.hsetnx("pick".getBytes(), name, indx);
			//pipe.hsetnx("indx".getBytes(), indx, name);			
			count+=1;
			if(count%1000==0) {
				pipe.sync();
				System.gc();
			}
		}
		pipe.sync();
		pipe.close();
		pool.close();
		
		final long t2 = System.currentTimeMillis();
		final String txt = Misc.tick2text(t2-t1,true,3);		
		Application.invokeLater(()->{
			System.out.printf("init done: %s\n",txt);
			msg.set("Init Done:"+txt);
		});
	}

	private void burn_space() {

		final long t1 = System.currentTimeMillis();
		
		final JedisPool pool = new JedisPool("localhost", 6379);

		final RandomDataGenerator rng = new RandomDataGenerator();

		final long bound = pck_size * CombinatoricsUtils.binomialCoefficient(tkn_size, pck_size);
		
		final AtomicLong total = new AtomicLong();
		
		while(total.get()<bound){
			final int task_size = 20;
			final ExecutorService exec = Executors.newFixedThreadPool(task_size);			
			for(int i=0; i<task_size; i++) {
				exec.execute(()->{
					final long cnt = 900000;
					for(long j=0; j<cnt; j++) {
						gen_sample(pool,rng);
					}
					total.addAndGet(cnt);
				});
			}
			try {
				exec.shutdown();
				exec.awaitTermination(8,TimeUnit.HOURS);
			} catch (InterruptedException e) {
				e.printStackTrace();
				break;
			}
			System.gc();
		}

		set_value(pool,"mcmc_total",total.get());
		
		pool.close();
		final long t2 = System.currentTimeMillis();
		final String txt = Misc.tick2text(t2-t1,true,3);
		Application.invokeLater(()->{
			System.out.printf("burn done: %s\n",txt);
			msg.set("Burn Done:"+txt);
		});
	}

	private void hist_space() {
		final long t1 = System.currentTimeMillis();
		
		final JedisPool pool = new JedisPool("localhost", 6379);

		final int task_size = 50;
		final ExecutorService exec = Executors.newFixedThreadPool(task_size);
		
		final long total = get_hlen(pool,"mcmc");
		final long chuck = total / task_size;//quotient
		
		final AtomicInteger[] count = new AtomicInteger[100];
		for(int i=0; i<count.length; i++) {
			count[i] = new AtomicInteger();
		}
		
		for(int i=0; i<=task_size; i+=1) {
			final long beg = 1 + i * chuck;
			final long end = (i!=task_size)?(beg + chuck - 1):(beg+total%task_size-1);
			System.out.printf("%08d-%08d\n", beg, end);
			exec.submit(()->{
				Jedis jedis = pool.getResource();
				for(long j=beg; j<=end; j+=1) {
					byte[] name = jedis.hget("indx".getBytes(), val2buf(j));
					byte[] buf = jedis.hget("mcmc".getBytes(), name);
					int cnt = buf2val(buf);
					if(cnt>count.length) {
						System.out.printf("bound=%d!!", cnt);
						cnt = count.length - 1;
					}
					count[cnt].incrementAndGet();
				}				
				jedis.close();
			});
		}		
		try {
			exec.shutdown();
			exec.awaitTermination(2,TimeUnit.HOURS);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		int[] hist = new int[count.length];
		int sum = 0;
		for(int i=0; i<count.length; i++) {
			int v = count[i].get();
			hist[i] = v;
			sum+=v;
		}
		Misc.serialize2file(count, "ggyy.obj");
		
		pool.close();
		final long t2 = System.currentTimeMillis();
		final String txt = Misc.tick2text(t2-t1,true,3);
		Application.invokeLater(()->{
			System.out.printf("hist done: %s",txt);
			msg.set("hist Done:"+txt);
		});
	}
	
	private void gen_sample(
		JedisPool pool, 
		RandomDataGenerator rnd
	) {
		ArrayList<Integer> tkn = new ArrayList<Integer>();
		for(int i=0; i<tkn_size; i++) {
			tkn.add(i);
		}
		int[] pck = new int[pck_size];
		for(int j=0; j<pck_size; j++) {
			int i = rnd.nextSecureInt(0, tkn.size()-1);
			pck[j] = tkn.remove(i);
		}
		Jedis jedis = pool.getResource();
		jedis.hincrBy("mcmc".getBytes(), pick2name(pck), 1L);
		jedis.close();
	}
	
	private void set_value(
		final JedisPool pool,
		final String key,final long val
	) {
		Jedis jedis = pool.getResource();
		jedis.set(key, String.valueOf(val));
		jedis.close();
	}
	
	private long get_hlen(
		final JedisPool pool,
		final String key
	) {
		Jedis jedis = pool.getResource();
		long size = jedis.hlen(key);
		jedis.close();
		return size;
	}
	
	private byte[] pick2name(int[] pck) {
		Arrays.sort(pck);
		byte[] buf = new byte[pck.length];
		for(int i=0; i<pck.length; i++) {
			buf[i] = (byte)(pck[i]+1);
		}
		return buf;
	}
	private int[] name2pick(byte[] name) {
		final int[] pck = new int[name.length];
		for(int i=0; i<name.length; i++) {
			pck[i] = name[i];
		}
		return pck;
	}
	private byte[] val2buf(final long val) {	
		//ByteBuffer buf = ByteBuffer.allocate(Long.BYTES);
		//buf.putLong(val);
		return String.valueOf(val).getBytes();
	}
	private byte[] val2buf(final int val) {		
		//ByteBuffer buf = ByteBuffer.allocate(Integer.BYTES);
		//buf.putInt(val);
		return String.valueOf(val).getBytes();
	}
	private int buf2val(final byte[] buf) {
		//ByteBuffer bb = ByteBuffer.wrap(buf);
		//bb.getInt();		
		return Integer.valueOf(new String(buf));
	}
	
	@Override
	public Node eventLayout(PanBase self) {
		final Label txt = new Label();
		txt.setMinSize(300, 75);
		txt.getStyleClass().add("font-size7");
		msg = txt.textProperty();

		final JFXButton btn = new JFXButton("!!start!!");
		btn.getStyleClass().add("btn-raised-1");
		btn.setMaxWidth(Double.MAX_VALUE);
		btn.setOnAction(e->{
			
		});
		stage().setOnShown(e->{
			new Thread(()->{
				init_space();
				//burn_space();
				//hist_space();
			},"all_space") .start();
		});
		return new BorderPane(txt,null,null,btn,null);
	}
}
