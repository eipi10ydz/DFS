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
 * @author 李亞希
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
		if (!Type.equals("LinkC")) {
			throw new NodeException("包的类型不对");
		} else if (!pac.containsKey("ID") || !pac.containsKey("Connectivity") || pac.size() != 2) {
			throw new NodeException("包的结构不对");
		} // 包的类型对了，但是结构不对
		pac.put("type", Type);
		json = gson.toJson(pac);
		return json;
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
		String json = new String();
		if (!Type.equals("LinkE")) {
			throw new NodeException("包的type类型不对");
		} else if (!Type_d.equals("01") && !Type_d.equals("04")) {
			throw new NodeException("包的type_d类型不对");
		} else if (!pac.containsKey("ID") || pac.size() != 1) {
			throw new NodeException("包的结构不对");
		}
		pac.put("type", Type);
		pac.put("type_d", Type_d);
		json = gson.toJson(pac);
		return json;
	}
	
	public Map<String, String> Receive_pac(String pac) throws NodeException{
		Gson gson = new Gson();
		Type t = new TypeToken<Map<String, String>>(){}.getType();
		Map<String, String> map = new HashMap<String,String>();
		try {
			map = gson.fromJson(pac, t);
		}
		catch(JsonSyntaxException e){
			System.err.println("解析失败");
		}
		if(map == null){
			throw new NodeException("包为空");
		}
		else if(!map.containsKey("type")){
			throw new NodeException("type类型不对");
		}
		switch(map.get("type")){
			case "LinkE" :
				if(!map.get("type_d").equals("03")){
					throw new NodeException("type_d类型不对");
				}
				else if(!map.containsKey("IP")||!map.containsKey("Port")||map.size() != 4){
					throw new NodeException("包的结构不对");
				}
				return map;
			case "ERR" :
				if(!map.get("type_d").equals("01")){
					throw new NodeException("type_d类型不对");
				}
				else if(map.size() != 2){
					throw new NodeException("包的结构不对");
				}
				return map;
			default :
				throw new NodeException("包的type类型不对");
				
		}
	}
	
	public Map<String, String> Check_table(String table) throws NodeException{
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
    	for(int i = 0 ;i < cnt ;i++){
         	String s3 = ""+i;
         	if(!map.containsKey(s1.concat(s3))||!map.containsKey(s2.concat(s3))){
         		throw new NodeException("包内容不全");
         	}
        }
    	return map;
	}	

	/**
	 * @param pac
	 * @return
	 */
	static public Map<String, String> unpack(String pac) {
		Gson gson = new Gson();
		Type t = new TypeToken<Map<String, String>>() {
		}.getType();
		Map<String, String> map = gson.fromJson(pac, t);
		return map;
	}

}