package nodetest;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 
 */

/**
 * @author lyx
 *
 */

public class Packer {
	/**
	 * @param Type
	 * @param pac
	 * @return
	 * @throws NodeException
	 */
	static public String pack(String Type, Map<String, String> pac) throws PackException {
		Gson gson = new Gson();
		String json = new String();
		switch(Type){
		 	case "LinkC" :
		 		if (!pac.containsKey("ID") || !pac.containsKey("ID_target") || 
		 								!pac.containsKey("Connectivity") || pac.size() != 3) {
					throw new PackException("包的结构不对");
		 		}
				else{
					pac.put("type", Type);
					json = gson.toJson(pac);
					return json;
				}
		 	case "Data" :
		 		if(!pac.containsKey("From")||!pac.containsKey("To")||!pac.containsKey("Content")||
		 																		!pac.containsKey("NO")){
		 			throw new PackException("包的结构不对");
		 		}
		 		else if(pac.size() != 4){
		 			throw new PackException("包的结构不对");
		 		}
		 		else{
		 			pac.put("type", Type);
					json = gson.toJson(pac);
					return json;
		 		}
		 	case "RoutI" :
		 		if(!pac.containsKey("ID")||!pac.containsKey("ID_target")||!pac.containsKey("RTT")
		 								||!pac.containsKey("bandwidth")||!pac.containsKey("lostRate")){
		 			throw new PackException("包的结构不对");
		 		}
		 		else if(pac.size() != 5){
		 			throw new PackException("包的结构不对");
		 		}
		 		else{
		 			pac.put("type", Type);
					json = gson.toJson(pac);
					return json;
		 		}
		 	case "RoutQ" :
		 		if(!pac.containsKey("ID")||!pac.containsKey("ID_target")||!pac.containsKey("Cnt")){
		 			throw new PackException("包的结构不对");
		 		}
		 		else if(pac.size() != 3){
		 			throw new PackException("包的结构不对");
		 		}
		 		else{
		 			pac.put("type", Type);
		 			json = gson.toJson(pac);
		 			return json;
		 		}
		 	case "DataF" :
		 		if(!pac.containsKey("From")||!pac.containsKey("To")||!pac.containsKey("No")){
		 			throw new PackException("包的结构不对");
		 		}
		 		else if(pac.size() != 3){
		 			throw new PackException("包的结构不对");
		 		}
		 		else{
		 			pac.put("type", Type);
		 			json = gson.toJson(pac);
		 			return json;
		 		}
		 	case "HEARTBEAT":
		 		if(!pac.containsKey("Cardiopalmus")||!pac.containsKey("Palpitation")){
		 			throw new PackException("包的结构不对");
		 		}
		 		else if(pac.size() != 2){
		 			throw new PackException("包的结构不对");
		 		}
		 		else{
		 			pac.put("type", Type);
		 			json = gson.toJson(pac);
		 			return json;
		 		}
		 	default:
		 		throw new PackException("type类型不对");
		}
	}

	/*
	 * {
	"type" : "DataF", //Finis  
	"From" : "", 
	"To" :   "", 
	"No" : 
} 
	 */
	/**
	 * @param Type
	 * @param Type_d
	 * @param pac
	 * @return
	 * @throws NodeException
	 */
	static public String pack(String Type, String Type_d, Map<String, String> pac) throws PackException {
		Gson gson = new Gson();
		String json = new String();
		switch(Type){
		 	case "LinkE" :
		 		switch(Type_d){
	 			case "01" :
	 				if (!pac.containsKey("ID")|| !pac.containsKey("ID_target") || pac.size() != 2) {
			 			throw new PackException("包的结构不对");
			 		}
			 		else{
						pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
			 		}
	 			case "04" :
	 				if (!pac.containsKey("ID") || pac.size() != 1) {
			 			throw new PackException("包的结构不对");
			 		}
			 		else{
						pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
			 		}
	 			case "02" :
	 				if (!pac.containsKey("ID") || !pac.containsKey("ID_target") || pac.size() != 2) {
			 			throw new PackException("包的结构不对");
			 		}
			 		else{
						pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
			 		}
	 			case "06" :
	 				if (!pac.containsKey("ID") || !pac.containsKey("ID_target") || pac.size() != 2) {
			 			throw new PackException("包的结构不对");
			 		}
			 		else{
						pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
			 		}
	 			case "07":
	 				if (!pac.containsKey("ID") || pac.size() != 1) {
			 			throw new PackException("包的结构不对");
			 		}
			 		else{
						pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
			 		}
	 			case "08":
	 				if (!pac.containsKey("ID") || pac.size() != 1) {
			 			throw new PackException("包的结构不对");
			 		}
			 		else{
						pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
			 		}
	 			default :
					throw new PackException("包的type_d类型不对");
		 		}
		 	case "NodeI" :
		 		switch(Type_d){
		 			case "00" :
		 				if(!pac.containsKey("Insertion") || pac.size() != 1){
		 					throw new PackException("包的结构不对");
		 				}
		 				else{
		 					pac.put("type", Type);
							pac.put("type_d", Type_d);
							json = gson.toJson(pac);
							return json;
		 				}
		 			case "03" :
		 				if(!pac.containsKey("UName") || !pac.containsKey("LIP")|| pac.size() != 2){
		 					throw new PackException("包的结构不对");
		 				}
		 				else{
		 					pac.put("type", Type);
							pac.put("type_d", Type_d);
							json = gson.toJson(pac);
							return json;
		 				}
		 			default :
						throw new PackException("包的type_d类型不对");
		 		}
		 	case "RoutD" :
		 		switch(Type_d){
		 			case "02":
		 				if(!pac.containsKey("From")||!pac.containsKey("To")||!pac.containsKey("No")
		 								||!pac.containsKey("NoBeg")){
		 					throw new PackException("包结构不对");
		 				}
		 				else if(!pac.containsKey("Cnt")||!pac.containsKey("PackCnt")){
		 					throw new PackException("包结构不对");
		 				}
		 				else if(!pac.containsKey("HopCnt")){
		 					throw new PackException("包结构不对");
		 				}
		 				else {
		 					 int Cnt = Integer.parseInt(pac.get("HopCnt"));
		 					 for(int i = 1 ; i <= Cnt ; i++){
		 						 if(!pac.containsKey("Hop_"+i)){
		 							throw new PackException("包的结构不对"); 
		 						 }
		 					 }
		 					 if(pac.size() != Cnt + 7){
		 						 throw new PackException("包结构不对");
		 					 }
		 				}
		 				pac.put("type", Type);
						pac.put("type_d", Type_d);
						json = gson.toJson(pac);
						return json;
		 			default :
		 				throw new PackException("type_d不对");
		 		}
		 	default:
		 		throw new PackException("type类型不对");
		}
	}
	
	static public boolean check_pack(Map<String, String> map){
		if(map == null){
			return false;
		}
		else if(!map.containsKey("type")){
			return false;
		}
		switch(map.get("type")){
			case "LinkE" :
				if(!map.containsKey("type_d")){
					return false;
				}
				else{
					switch(map.get("type_d")){
						case "03":
							if(!map.containsKey("IP")||!map.containsKey("Port")||!map.containsKey("ID")
																	||map.size() != 5){
								return false;
							}
							else
							return true;
						case "05":
							if(!map.containsKey("content")||map.size() != 3){
								return false;
							}
							else
							return true;
						case "06" :
							if(!map.containsKey("ID_target")||!map.containsKey("ID")||map.size() != 4){
								return false;
							}
							else
								return true;
						case "07" :
							if(!map.containsKey("ID")||map.size() != 3){
								return false;
							}
							else
								return true;
						case "08" :
							if(!map.containsKey("ID")||map.size() != 3){
								return false;
							}
							else
								return true;
						default :
							return false;
					}
				}
			case "ERR" :
				if(!map.containsKey("type_d")){
					return false;
				}
				else if(!map.get("type_d").equals("01")){
					return false;
				}
				else if(map.size() != 2){
					return false;
				}
				return true;
			case "NodeI":
				if(!map.containsKey("type_d")){
					return false;
				}
				switch (map.get("type_d")){
					case "01" :
						if(!map.containsKey("ID")|| map.size() != 3){
							return false;
						}
						return true;
					case "02":
						if(!map.containsKey("ID")||!map.containsKey("UName")
								||!map.containsKey("LIP")|| map.size() != 5){
							return false;
						}
						return true;
					default :
						return false;
				}
			case "NodeD":
				if(!map.containsKey("IP")||map.size()!= 2){
					return false;
				}
				return true;
			case "Data":
				if(!map.containsKey("No")||!map.containsKey("Content")){
					return false;
				}
				else if(map.size() != 5){
					return false;
				}
				return true;
			case "HEARTBEAT" :
				if(!map.containsKey("Cardiopalmus")||!map.containsKey("Palpitation")){
					return false;
				}
				else if(map.size() != 3){
					return false;
				}
				return true;
			case "DataF" :
				if(!map.containsKey("From")||!map.containsKey("To")||!map.containsKey("No")){
					return false;
				}
				else if(map.size() != 4){
					return false;
				}
				return true;
			default :
				return false;	
		}
	}
	
	static public Map<String, String> Check_table(String table) throws NodeException{
		Gson gson = new Gson();
		Type t = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> map = new ConcurrentHashMap<String,String>();
		try{
			map = gson.fromJson(table,t);
		}
		catch(JsonSyntaxException e){
			e.printStackTrace();
		}
		if(map == null){
			throw new NodeException("表为空");
		}
		else if (!map.containsKey("type") ){
			throw new NodeException("type类型不对");
		}
		else if (!map.get("type").equals("NodeT")){
			throw new NodeException("type类型不对");
		}
		else if (!map.containsKey("cnt") ){
			throw new NodeException("无cnt");
		}
        int cnt = Integer.parseInt(map.get("cnt"));
        if(map.size() != 2+cnt*3 ){
        	throw new NodeException("内容数量不对");
        }
    	for(int i = 1 ;i <= cnt ;i++){
         	if(!map.containsKey("UName_" + i)||!map.containsKey("ID_" + i)||!map.containsKey("LIP_" + i)){
         		throw new NodeException("包内容不全");
         	}
        }
    	return map;
	}	
	
	static public Map<String, String> check_RoutD(String RoutD,String ID) throws NodeException{
		Gson gson = new Gson();
		Type t = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> map = new ConcurrentHashMap<String,String>();
		try{
			map = gson.fromJson(RoutD,t);
		}
		catch(JsonSyntaxException e){
			e.printStackTrace();
		}
		if(map == null){
			throw new NodeException("表为空");
		}
		else if (!map.containsKey("type") ){
			throw new NodeException("type类型不对");
		}
		else if (!map.get("type").equals("RoutD")){
			throw new NodeException("type类型不对");
		}
		else if(!map.containsKey("type_d")){
			throw new NodeException("type_d类型不对");
		}
		switch(map.get("type_d")){
			case "01":
				if(!map.containsKey("From")){
					throw new NodeException("包的信息不完整");
				}
				else if(map.get("From") != ID){
					throw new NodeException("信息错误");
				}
				else if(!map.containsKey("To")){
					throw new NodeException("包的信息不完整");
				}
				else if(!map.containsKey("RoutCnt")){
					throw new NodeException("包的信息不完整");
				}
		        int RoutCnt = Integer.parseInt(map.get("RoutCnt"));
		        String s1 = new String("Hop_");
		        String s2 = new String("Rout");
		    	for(int i = 1 ;i <= RoutCnt ;i++){
		    		if(!map.containsKey(s2 + i)){
		    			throw new NodeException("包的信息不完整");
		    		}
		    		else if(!map.containsKey(s2 + i + "Cnt")){
		    			throw new NodeException("包的信息不完整");
		    		}
		    		int RoutiCnt = Integer.parseInt(map.get(s2 + i + "Cnt"));
		    		for(int j = 1; j <= RoutiCnt ; j++){
		    			if(!map.containsKey(s2 + i + s1 + j )){
		             		throw new NodeException("包内容不全");
		             	}
		    		}
		        }
		    	return map;
			case "02" :
				if(!map.containsKey("From")){
					throw new NodeException("包的信息不完整");
				}
				else if(!map.containsKey("To")){
					throw new NodeException("包的信息不完整");
				}
				else if(map.get("To") != ID){
					throw new NodeException("包不对");
				}
				else if(!map.containsKey("No")){
					throw new NodeException("包的信息不完整");
				}
				else if(!map.containsKey("NoBeg")){
					throw new NodeException("包的信息不完整");
				}
				else if(!map.containsKey("PackCnt")){
					throw new NodeException("包的信息不完整");	
				}
				else if(!map.containsKey("Cnt")){
					throw new NodeException("包的信息不完整");	
				}
				else if(!map.containsKey("HopCnt")){
					throw new NodeException("包的信息不完整");	
				}
		        int Cnt = Integer.parseInt(map.get("HopCnt"));
		        String s3 = new String("Hop_");
		    	for(int i = 1 ;i <= Cnt ;i++){
		    		if(!map.containsKey(s3 + i)){
		             	throw new NodeException("包内容不全");
		            }
		    	}
		    	return map;
		    default:
		    	throw new NodeException("包的信息错误");
		}
	}	

	
	/**
	 * @param pac
	 * @return
	 * @throws NodeException
	 */
	static public Map<String, String> unpack(String pac) throws PackException{
		Gson gson = new Gson();
		Type t = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> map = new ConcurrentHashMap<String,String>();
		try {
			map = gson.fromJson(pac, t);
		}
		catch(JsonSyntaxException e){
			throw new PackException("解析失败");
		}
		return map;
	}
}