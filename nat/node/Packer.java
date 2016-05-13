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
 * @author �ϣ
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
			throw new NodeException("�������Ͳ���");
		} else if (!pac.containsKey("ID") || !pac.containsKey("Connectivity") || pac.size() != 2) {
			throw new NodeException("���Ľṹ����");
		} // �������Ͷ��ˣ����ǽṹ����
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
			throw new NodeException("����type���Ͳ���");
		} else if (!Type_d.equals("01") && !Type_d.equals("04")) {
			throw new NodeException("����type_d���Ͳ���");
		} else if (!pac.containsKey("ID") || pac.size() != 1) {
			throw new NodeException("���Ľṹ����");
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
			System.err.println("����ʧ��");
		}
		if(map == null){
			throw new NodeException("��Ϊ��");
		}
		else if(!map.containsKey("type")){
			throw new NodeException("type���Ͳ���");
		}
		switch(map.get("type")){
			case "LinkE" :
				if(!map.get("type_d").equals("03")){
					throw new NodeException("type_d���Ͳ���");
				}
				else if(!map.containsKey("IP")||!map.containsKey("Port")||map.size() != 4){
					throw new NodeException("���Ľṹ����");
				}
				return map;
			case "ERR" :
				if(!map.get("type_d").equals("01")){
					throw new NodeException("type_d���Ͳ���");
				}
				else if(map.size() != 2){
					throw new NodeException("���Ľṹ����");
				}
				return map;
			default :
				throw new NodeException("����type���Ͳ���");
				
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
			throw new NodeException("��Ϊ��");
		}
		else if (!map.containsKey("type") ){
			throw new NodeException("type���Ͳ���");
		}
		else if (!map.get("type").equals("NodeT")){
			throw new NodeException("type���Ͳ���");
		}
		else if (!map.containsKey("cnt") ){
			throw new NodeException("��cnt");
		}
		String count = new String();
		count = map.get("cnt");
        int cnt = Integer.parseInt(count);
        if(map.size() != 2+cnt*2 ){
        	throw new NodeException("������������");
        }
        String s1 = new String("UName_"); 
        String s2 = new String("ID_");
    	for(int i = 0 ;i < cnt ;i++){
         	String s3 = ""+i;
         	if(!map.containsKey(s1.concat(s3))||!map.containsKey(s2.concat(s3))){
         		throw new NodeException("�����ݲ�ȫ");
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