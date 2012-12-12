import java.io.File;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import groovy.io.FileType;

class MethodRule {
	
		String signature;
		String new_signature;
		String name;
		String name_sig;
		List new_method_params;
		List old_method_params;
		
		public int getOldParamsSize(){
			return old_method_params.size();
		}
		
		public int getNewParamsSize(){
			return new_method_params.getArr().size();
		}
}

def getAllWidgettiFiles(){
	def arr=[];
	iterateOverDirectory(new File("/ecost/devel3/widgetti-ecost/src"),arr);
	iterateOverDirectory(new File("/ecost/devel3/widgetti-v5/src"),arr);
	return arr;
}

def iterateOverDirectory(File dir,def arr){
	dir.eachFileRecurse(FileType.FILES) {
		file->
		arr<<file;
	}
	return arr;
}
def getParams(def signature){
	def m=pattern.matcher(signature);
	def arr=[];
	if(m.find()){
		int num=m.groupCount();
		if(num>0){
			def param_list=m.group(num);
			def param_split=param_list.split(",");
			param_split.each{
				arr<<it.trim();
			}
		}
	}
	return arr;
}

def breakDownNameSig(def str){
	def arr=str.split(" ");
	def strFullSig=''; //define string here
	def lastItem=arr.last();
	arr.each {
		temp->
		temp=temp.trim();
		//println "strFullsig type is ${strFullSig.getClass()}";
		if(!temp.isEmpty()){
			strFullSig<<=temp;
			if(!temp.equalsIgnoreCase(lastItem)){
				strFullSig<<="[\\s]+";
			}
		}
	}
	return strFullSig;
}

pattern=Pattern.compile("\\((.*)\\)");
def records = new XmlSlurper().parseText(new File("src/method_rules.xml").text)
def arrFiles=getAllWidgettiFiles();
def set=[];

def arr=[];
records.method_rule.each {
	arr<<new MethodRule(name_sig:breakDownNameSig(it.name_sig.toString()),signature:it.signature,new_signature:it.new_signature,name:it.name,
						new_method_params:getParams(it.new_signature.toString()),old_method_params:getParams(it.signature.toString()));
};

//def diff=new File("src/file_names.txt");
//diff.text="";

arrFiles.each{
	 f->
	arr.each{
			obj->
			def bool=(f.text=~obj.name_sig).find();
			if(bool){
				//println " file name is $file and the signature is $obj.name_sig";
				//diff.append "$f\n";
				f.eachLine { txt,num ->
					//fine the line that contains the sig
					if((txt=~obj.name_sig).find()){
						arrTemp=getParams(txt);
						if(arrTemp.size()==0 || arrTemp.size() != obj.getOldParamsSize()){
							set<<new MethodBean(name:obj.name_sig,line:num, filename:f);
							println "${obj.name_sig}, ${num}, ${f}";
						}
					}
						
				}
			}
	}
}

processSet(set);
println 'done with formatting'

def processSet(def set){
	if(!set.isEmpty()){
		
	set.each{
		obj->
		//"$obj.name, $obj.line, $obj.filename";
		def file=obj.filename;
		def end_line;
		def before='',after='';
		def start=false;
		def list_txt=[];
		file.eachLine{
			txt,ctr ->
			list_txt<<=txt;
			if(start){
				if(txt.contains("{")){
					start=false;
					before<<="\n"
					before<<=txt;
					end_line=ctr;
				}else{
					before<<="\n"
					before<<=txt;
				}
			}else{
				if(ctr==obj.line){
					//start now
					before<<=txt;
					start=true;
				}
			}
			
		}
		def range=obj.line..end_line;
		def local_text=file.text;
		if(local_text.contains(before)){
			//replace new line and tab
			after=before.replaceAll("\n","").replaceAll("\t","");
			range.each {
				i->
				if(i==obj.line){
					list_txt[i-1]=after;
				}else{
					list_txt[i-1]='';
				}
			}
			//println "local text is now: ${list_txt.join('\n')}"
			file.text=list_txt.join('\n');
		}
	}
  }
}

