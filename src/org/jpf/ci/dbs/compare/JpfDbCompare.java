/** 
 * @author 吴平福 
 * E-mail:wupf@asiainfo.com 
 * @version 创建时间：2015年2月14日 上午1:26:12 
 * 类说明 
 */

package org.jpf.ci.dbs.compare;

import java.sql.Connection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jpf.utils.JpfDbUtils;
import org.jpf.utils.JpfFileUtil;
import org.jpf.utils.SvnInfoUtil;
import org.jpf.xmls.JpfXmlUtil;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 */
public class JpfDbCompare
{
	private static final Logger logger = LogManager.getLogger();
	private static  HashMap<String,String> parent_child=new HashMap<String,String>();
	public static HashMap<String,String> getParentChild()
	{
		return parent_child;
	}
	/**
	 * 
	 */
	public JpfDbCompare(String strConfigFileName)
	{
		// TODO Auto-generated constructor stub
		try
		{
			// read config
			//System.out.println(System.getProperty("user.dir"));
			//System.out.println(System.getProperty("java.class.path"));
			DbDescInfo cPdmDbDescInfo = null;
			CompareInfo cCompareInfo=new CompareInfo();
			
			JpfFileUtil.CheckFile(strConfigFileName);
			NodeList nl = JpfXmlUtil.GetNodeList("dbsource", strConfigFileName);
			logger.debug(nl.getLength());
			String strDefaultMail=""; 
			String strPdmInfo="";
			if(1==nl.getLength())
			{
				Element el = (Element) nl.item(0);
				String strJdbcUrl = JpfXmlUtil.GetParStrValue(el, "dburl");
				String strDbUsr = JpfXmlUtil.GetParStrValue(el, "dbusr");
				String strDbPwd = JpfXmlUtil.GetParStrValue(el, "dbpwd");
				strDefaultMail= JpfXmlUtil.GetParStrValue(el, "dbmails");
				strPdmInfo= JpfXmlUtil.GetParStrValue(el, "svnurl");
				cCompareInfo.setStrExcludeTable(JpfXmlUtil.GetParStrValue(el, "excludetable"));
				cCompareInfo.setStrCondName(JpfXmlUtil.GetParStrValue(el, "envname"));
				cCompareInfo.setDoExec(JpfXmlUtil.GetParStrValue(el, "doexecsql"));
				cCompareInfo.setStrPdmInfo(strJdbcUrl);
				if (strPdmInfo!=null)
				{
					strPdmInfo=SvnInfoUtil.GetSvnFileAuthorDate(strPdmInfo);
				}
				logger.debug(strJdbcUrl);
				logger.debug(strDbUsr);
				logger.debug(strDbPwd);
				cPdmDbDescInfo = new DbDescInfo(strJdbcUrl, strDbUsr, strDbPwd);
			}else {
				logger.error("error source db info");
			}
			nl = JpfXmlUtil.GetNodeList("dbcompare", strConfigFileName);
			logger.debug(nl.getLength());
			for (int j = 0; j < nl.getLength(); j++)
			{
				// System.out.println(nl.item(j).getNodeValue());
				Element el = (Element) nl.item(j);
				String strJdbcUrl = JpfXmlUtil.GetParStrValue(el, "dburl");
				String strDbUsr = JpfXmlUtil.GetParStrValue(el, "dbusr");
				String strDbPwd = JpfXmlUtil.GetParStrValue(el, "dbpwd");
				String strDomain = JpfXmlUtil.GetParStrValue(el, "dbdomain");
				String strMails = JpfXmlUtil.GetParStrValue(el, "dbmails")+","+strDefaultMail;
				NodeList nlParentChild= el.getElementsByTagName("parent_childen");
				
				//获得需要进行特殊处理的分表——母表对应关系
				parent_child.clear();
				for (int i=0; i<nlParentChild.getLength(); i++){
		               Node child = nlParentChild.item(i);
		               if (child instanceof Element)
		               {
		                   String s = child.getFirstChild().getNodeValue().toLowerCase().trim();
		                   String[]s1=s.split(";");
		                   if(s1.length==2)
		                   {
		                	   parent_child.put(s1[1], s1[0]);   
		                   }
		               }
				 }
				
				Iterator mapite=parent_child.entrySet().iterator();
				 while(mapite.hasNext())
				 {
					Map.Entry testDemo=(Map.Entry)mapite.next();
					Object key=testDemo.getKey();
					Object value=testDemo.getValue();
					System.out.println(key+"-------"+value);
				 }	
				logger.debug(strDomain);
				logger.debug(strJdbcUrl);
				logger.debug(strDbUsr);
				logger.debug(strDbPwd);
				logger.debug(strMails);


				DbDescInfo cDbDescInfo2 = new DbDescInfo(strJdbcUrl, strDbUsr, strDbPwd);
				
				Connection conn_pdm = null;
				Connection conn_develop = null;
				try
				{
					conn_pdm =  cPdmDbDescInfo.GetConn();
					conn_develop = cDbDescInfo2.GetConn();
					
					cCompareInfo.setStrJdbcUrl(strJdbcUrl+"/"+strDomain);
					cCompareInfo.setStrDomain(strDomain);
					cCompareInfo.setStrMails(strMails);
					
			
					//带分表比对
					System.out.println(".....................................................................................................................");
					System.out.println("Check sub table...");
					CompareSubTables cCompareSubTables = new CompareSubTables();
					cCompareSubTables.DoCompare(conn_pdm, conn_develop, cCompareInfo);
					
					// 带分表比较索引
					System.out.println(".....................................................................................................................");
					System.out.println("compare sub index...");
					
					CompareIndexSub cCompareIndexSub= new CompareIndexSub();
					cCompareIndexSub.DoCompare(conn_pdm, conn_develop, cCompareInfo);

					// 比较表
					//System.out.println(".....................................................................................................................");
					//System.out.println("compare tables...");
					//CompareTable cCompareTable = new CompareTable();
					//cCompareTable.DoCompare(conn_pdm, conn_develop, strDomain,strMails,strJdbcUrl+"/"+strDomain,strPdmInfo,strExcludeTable);
					
					// 比较索引
					//System.out.println(".....................................................................................................................");
					//System.out.println("compare index...");
					//CompareIndex cCompareIndex = new CompareIndex();
					//cCompareIndex.DoCompare(conn_pdm, conn_develop, strDomain,strMails,strJdbcUrl+"/"+strDomain,strPdmInfo,strExcludeTable);
		
					// 检查是否母表是否存在
					//logger.info(".....................................................................................................................");
					//logger.info("CheckParentTableExist...");
					//logger.debug("conn_product.isClosed()="+conn_product.isClosed());
					//CheckParentTableExist cCheckParentTableExist = new CheckParentTableExist();
					//cCheckParentTableExist.DoCheck(conn_product,strDomain);
					//cCheckParentTableExist.DoCheck(conn_develop,strDomain);
					
					//System.out.println(".....................................................................................................................");
					//System.out.println("CheckSameTableName...");
					//CheckSameTableName cCheckSameTableName = new CheckSameTableName();
					//cCheckSameTableName.DoCheck(conn_product,strDomain);
					//cCheckSameTableName.DoCheck(conn_develop,strDomain);
					
					//System.out.println(".....................................................................................................................");
					//System.out.println("Compare Data...");
					//CompareData cCompareData = new CompareData();
					//cCompareData.DoCompare(conn_pdm, conn_develop, strConfigFileName);
					

					
				} catch (Exception ex)
				{
					// TODO: handle exception
					ex.printStackTrace();
				} finally
				{
					JpfDbUtils.DoClear(conn_pdm);
					JpfDbUtils.DoClear(conn_develop);
				}
			}
		} catch (Exception ex)
		{
			// TODO: handle exception
			ex.printStackTrace();
		}
		logger.info("game over");
	}

	/**
	 * @param args
	 *            被测试类名：TODO 被测试接口名:TODO 测试场景：TODO 前置参数：TODO 入参： 校验值： 测试备注：
	 *            update 2015年2月14日
	 */
	public static void main(String[] args)
	{
		// TODO Auto-generated method stub
		if (1 == args.length)
		{
			JpfDbCompare cJpfDbCompare = new JpfDbCompare(args[0]);
		}
	}

}
