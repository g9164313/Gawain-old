//Step.0: Ensure we selected the right page
mo.click(255,68);
mo.click(55,174);

//Step.?:關閉冷凝泵

//step.1:打開主泵，等10秒
mo.click(285,686);
mo.click(406,438);
mo.delay_sec(10);

//step.2: 主閥打開(等到機器聲音出來)
mo.click(433,524);
mo.click(410,427);
mo.delay_sec(3);

//step.3: 看上方壓力直到(4.5e-2 Torr)
torr = mo.recognize(870,221,75,23);
while(torr>=4.5e-2){
	torr = mo.recognize(870,221,75,23);
	mo.delay_sec(1);
	print("check pressure...")
}

//step.4: 關閉主閥(等到機器聲音出來)
mo.click(436,523);
mo.click(516,440);
mo.delay_sec(5);

//step.6: 關閉主泵
mo.click(300,678);
mo.click(522,434);
mo.delay_sec(10);

//step.7: 開啟冷凝泵
mo.click(788,448);
mo.click(407,442);
mo.delay_sec(3);

//step.8: 看下方壓力直到(4.5e-6 Torr)
torr = mo.recognize(870,268,75,23);
while(torr>=4.5e-6){
	torr = mo.recognize(870,268,75,23);
	mo.delay_sec(1);
}

//step.9: 關閉冷凝泵
//mo.click(785,453);
//mo.click(515,438);

//Finally, we done~~~~~
print("Done!!");







