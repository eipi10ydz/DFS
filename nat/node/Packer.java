import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;


/**
 * 
 */

/**
 * @author 李亞希, ydz
 *
 */

public class Packer {
	/**
	 * @param Type
	 * @param pac
	 * @return
	 * @throws NodeException
	 */
	static public String pack(String Type, Map<String, String> pac) throws NodeException {
		Gson gson = new Gson();
		String json = new String();
		switch(Type){
		 	case "LinkC" :
		 		if (!pac.containsKey("ID") || !pac.containsKey("Connectivity") || pac.size() != 2) {
					throw new NodeException("包的结构不对");
		 		}
				else{
					pac.put("type", Type);
					json = gson.toJson(pac);
					return json;
				}
		 	case "Data" :
		 		if(!pac.containsKey("From")||!pac.containsKey("To")||!pac.containsKey("Content")){
		 			throw new NodeException("包的结构不对");
		 		}
		 		else if(pac.size() != 3){
		 			throw new NodeException("包的结构不对");
		 		}
		 		else{
		 			pac.put("type", Type);
					json = gson.toJson(pac);
					return json;
		 		}
		 	default:
		 		throw new NodeException("type类型不对");
		}
	}

	/**
	 * @param Type
	 * @param Type_d
	 * @param pac
	 * @return
	 * @throws NodeException
	 */
	static public String pack(String Type, String Type_d, Map<String, String> pac) throws NodeException {
		Gson gson = new Gson();
		String json;
		if (!Type.equals("LinkE") && !Type.equals("NodeI")) {
			throw new NodeException("包的type类型不对");
		} else if (Type.equals("LinkE") && !Type_d.equals("01") && !Type_d.equals("04") || Type.equals("NodeI") && !Type_d.equals("00") && !Type_d.equals("03")) {
			throw new NodeException("包的type_d类型不对");
		} else if ((Type.equals("LinkE") && !pac.containsKey("ID")) || ((Type.equals("NodeI") && Type_d.equals("00") && !pac.containsKey("Insertion")) || Type.equals("NodeI") && Type_d.equals("03") && !pac.containsKey("UName")) || pac.size() != 1 ) {
			throw new NodeException("包的结构不对");
		}
		pac.put("type", Type);
		pac.put("type_d", Type_d);
		json = gson.toJson(pac);
		return json;
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
			    else if(!map.get("type_d").equals("03")){
					return false;
				}
				else if(!map.containsKey("IP")||!map.containsKey("Port")||map.size() != 4){
					return false;
				}
				return true;
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
				else if(!map.get("type_d").equals("01")&&!map.get("type_d").equals("02")){
					return false;
				}
				else if(!map.containsKey("ID")||map.size() != 3){
					return false;
				}
				return true;
			case "NodeD":
				if(!map.containsKey("IP")||map.size()!= 2){
					return false;
				}
				return true;
			case "Data":
				if(!map.containsKey("From")||!map.containsKey("To")||!map.containsKey("Content")){
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
		Map<String, String> map = new HashMap<String,String>();
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
		String count = new String();
		count = map.get("cnt");
        int cnt = Integer.parseInt(count);
        if(map.size() != 2+cnt*2 ){
        	throw new NodeException("内容数量不对");
        }
        String s1 = new String("UName_"); 
        String s2 = new String("ID_");
    	for(int i = 1 ;i <= cnt ;i++){
         	if(!map.containsKey(s1+i)||!map.containsKey(s2+i)){
         		throw new NodeException("包内容不全");
         	}
        }
    	return map;
	}	

	/**
	 * @param pac
	 * @return
	 * @throws NodeException
	 */
	static public Map<String, String> unpack(String pac) throws NodeException{
		Gson gson = new Gson();
		Type t = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> map = new HashMap<String,String>();
		try {
			map = gson.fromJson(pac.trim(), t);
		}
		catch(JsonSyntaxException e){
			throw new NodeException("解析失败");
		}
		return map;
	}
}