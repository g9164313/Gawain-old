/*
 * main5.cpp
 *
 *  Created on: 2016年8月11日
 *      Author: qq
 */
#include <stdio.h>
#include <stdlib.h>
#include <iostream>
#include <fstream>
#include <sstream>
#include <list>
#include <map>
#include <string>
#include <vector>
#include <sstream>
#include <algorithm>
#include <iterator>

using namespace std;

/**
 * Put result here, it is key-value pair!!!
 */
vector<map<string,string> > vecToken;


string trim(string& str){
	size_t fst,lst;

	lst = str.find_first_of('\r');
	if(lst==string::npos){
		return str;
	}
	str = str.substr(0,lst);

    fst = str.find_first_not_of(' ');
    if(fst==string::npos){
    	return str;
    }
    lst = str.find_last_not_of(' ');
    if(lst==string::npos){
    	return str;
    }
    return str.substr(fst, (lst-fst+1));
}

void parseProp(
	map<string,string>& map,
	string& txt,
	istream& fs,
	bool chkDot
){
	size_t pos = txt.find_first_of('=');
	if(pos==string::npos){
		return;
	}
	string key = txt.substr(0,pos);
	string val = txt.substr(pos+1);
	if(key[0]=='$'){
		//special case~~~
		map[key] = val;
		return;
	}
	if(chkDot==true){
		string tmp = val;
		for(;;){
			pos = tmp.find_last_of('.');
			if(pos!=string::npos){
				val = val.substr(0,val.length()-1);
				break;
			}
			getline(fs,tmp);//???
			val = val + trim(tmp);
		}
	}
	map[key] = val;
}

void parseToken(istream& fs){

	vecToken.clear();

	vecToken.push_back(map<string,string>());//SYSTEM
	vecToken.push_back(map<string,string>());//PARAMETER
	vecToken.push_back(map<string,string>());//OS_NET
	vecToken.push_back(map<string,string>());//SILVER

	string txt,tag;

	while(getline(fs,txt)){

		txt = trim(txt);

		if(txt[0]=='%'){

			tag = txt.substr(1);//update tag~~~~

			if(tag=="END"){
				return;
			}

		}else if(txt[0]=='#'){

			continue;//this is comment, skip this~~~

		}else if(tag=="SYSTEM"){

			parseProp(vecToken[0],txt,fs,false);

		}else if(tag=="PARAMETER"){

			parseProp(vecToken[1],txt,fs,false);

		}else if(tag=="OS_NET"){

			parseProp(vecToken[2],txt,fs,true);

		}else if(tag=="SILVER"){

			parseProp(vecToken[3],txt,fs,true);

		}else{
			cerr<<"NO_SUPPORT@"<<tag<<" - "<<txt;
		}
	}
}

void takeValue(string& src,vector<int>& dst){
	dst.clear();
	stringstream ss(src);
	string tmp;
	while(getline(ss,tmp,',')){
		dst.push_back(atoi(tmp.c_str()));
	}
}
//------------------//

int main(int argc, char* argv[]) {

	ifstream fs("906001AS4.Net");

	parseToken(fs);

	//below code is example~~~
	map<string,string> hash = vecToken[2];

	cout<<"--List OS_Net--"<<endl;
	map<string,string>::iterator it;
	for(it=hash.begin(); it!=hash.end(); ++it){
		cout << (it->first) << " => " << (it->second) << '\n';
	}

	cout<<"--Take Net--"<<endl;
	cout<<"NET43 => "<<hash["NET43"]<<endl;

	cout<<"--Take Value--"<<endl;
	vector<int> vals;
	takeValue(hash["NET43"],vals);
	for(int i=0; i<vals.size(); i++){
		cout<<vals[i]<<",";
	}
	cout<<endl;

	return 0;
}



