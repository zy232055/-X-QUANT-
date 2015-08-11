package com.dfitc.stpe89a8f39d67e.strategy.baseStrategy;

import com.dfitc.stp.annotations.*;
import com.dfitc.stp.indicator.*;
import com.dfitc.stp.market.*;
import com.dfitc.stp.trader.*;
import com.dfitc.stp.strategy.*;
import com.dfitc.stp.entity.Time;
import com.dfitc.stp.util.BarsUtil;
import com.dfitc.stp.util.StringUtil;
import java.util.Date;



/**
  *策略描述:R-Breaker策略基本原理
	R-Breaker 是一种短线交易策略，它结合了趋势和反转两种交易方式。 其基本原理如下：
	1. 根据前一个交易日的收盘价、最高价和最低价数据通过一定方式计算出六个价位，从大到小依次为：突破买入价、观察卖出价、反转卖出价、反转买入价、观察买入价、突破卖出价。
	由这六个价格来形成当前交易日盘中交易的触发条件。对计算方式的调整，可以调节六个价格间的距离，进一步改变触发条件。
	2. 追踪盘中价格走势，实时判断触发条件。具体条件如下：
	当日内最高价超过观察卖出价后，盘中价格出现回落，且进一步跌破反转卖出价构成的支撑线时，采取反转策略，即在该点位（反手、开仓）做空；
	当日内最低价低于观察买入价后，盘中价格出现反弹，且进一步超过反转买入价构成的阻力线时，采取反转策略，即在该点位（反手、开仓）做多；
	在空仓的情况下，如果盘中价格超过突破买入价，则采取趋势策略，即在该点位开仓做多；
	在空仓的情况下，如果盘中价格跌破突破卖出价，则采取趋势策略，即在该点位开仓做空。
	3. 设定止损条件。当亏损达到设定值后，平仓。
	4. 设定过滤条件。当前一个交易日波幅过小，该交易日不进行交易。
	5. 在每日收盘前，对所持合约进行平仓。
	6. 可使用 1 分钟、 5 分钟或 10 分钟等高频数据进行判断。
	@author 张宇 2015-08-11 zhangyuchnqd@163.com
  */
	
	
@Strategy(name = "R_Breaker",version="1.0",outputMode = OutputMode.TIMER, outputPeriod = 3000, contractNumber = 1)
public class R_Breaker extends BaseStrategy {
	/**
	 * 参数描述:
	 */

	@In(label = "开仓手数", sequence = 0)
	@Text(value = "1", readonly = false)
	int vol;
	
	

	@In(label = "尾盘强制出场时间", sequence = 1)
	@DateTime(value = "14:55:00", format = "HH:mm:ss", style = Style.TIME)
	Time exitTime;
	
	

	@In(label = "观察买入卖出系数", sequence = 2)
	@Text(value = "0.35", readonly = false)
	double f1;
	
	

	@In(label = "突破买入卖出系数", sequence = 3)
	@Text(value = "0.25", readonly = false)
	double f3;
	
	

	@In(label = "反转系数", sequence = 4)
	@Text(value = "0.07", readonly = false)
	double f2;
	
	/** 
 * 参数描述:根据前一个交易日的收盘价、最高价和最低价数据通过一定方式计算出六个价位，
 * 从大到小依次为：突破买入价（Bbreak)、观察卖出价(Ssetup)、反转卖出价(Senter)、
 * 反转买入价(Benter)、观察买入价(Bsetup)、突破卖出价(Sbreak)。
 */
double bBreak=0;
	double sSetup=0;
	double sEnter=0;
	double bEnter=0;
	double bSetup=0;
	double sBreak=0;
	double lastDayOpenPrice=0;
	BarsUtil barsUtil=new BarsUtil(100);

	
	
 	/**
	 
 * 初始化K线周期，在策略创建时被调用(在initialize之后调用)	
 * @param contracts策略相关联的合约
	 */
	@Override
	public void setBarCycles(String[] contracts) {
		 importBarCycle(contracts[0], Unit.MINUTE, 1);
	}

	/**
	 
 * 初始化指标，在策略创建时被调用(在initialize之后调用)	
 * @param contracts策略相关联的合约
	 */
	@Override
	public void setIndicators(String[] contracts) {
		
	}
	
	/**
	 * 初始化方法，在策略创建时调用
	 * 
	 * @param contracts
	 *            策略关联的合约
	 */
	@Override
	public void initialize(String[] contracts) {
		this.setAutoPauseBySystem(false);
		this.setAutoResumeBySystem(false);
		this.setAutoPauseByLimit(false);
		this.setExitOnClose(exitTime);
	}
	/**
	 * 处理K线
	 * 
	 * @param bar
	 *            触发此次调用的K线
	 * @param barSeries
	 *            此次K线所对应的K线序列(barSeries.get()与bar是等价的)
	 */
	public void processBar(Bar bar, BarSeries barSeries) {
		barsUtil.update(barSeries);
		//System.out.println("barsUtil.openD(0) " + barsUtil.openD(0));
		/**
		 * 	观察卖出价:昨高+0.35*(昨收-昨低);//ssetup
		  	反转卖出价:(1.07/2)*(昨高+昨低)-0.07*昨低;//senter
			反转买入价:(1.07/2)*(昨高+昨低)-0.07*昨高;//benter
			观察买入价:昨低-0.35*(昨高-昨收);//bsetup
			突破买入价:(观察卖出价+0.25*(观察卖出价-观察买入价));//bbreeak
			突破卖出价:观察买入价-0.25*(观察卖出价-观察买入价);//sbreak
		 */
			if (barsUtil.openD(0) != lastDayOpenPrice){
				lastDayOpenPrice = barsUtil.openD(0);		
				sSetup = barsUtil.highD(1) + f1 * (barsUtil.closeD(1) - barsUtil.lowD(1));
				sEnter = ((1 + f2)/2) * (barsUtil.highD(1) + barsUtil.lowD(1)) - f2 * barsUtil.lowD(1);
				bEnter = ((1 + f2)/2) * (barsUtil.highD(1) + barsUtil.lowD(1)) - f2 * barsUtil.highD(1);
				bSetup = barsUtil.lowD(1) - f1 * (barsUtil.highD(1) - barsUtil.closeD(1));;
				bBreak = sSetup + f3 * (sSetup - bSetup);
				sBreak = bSetup - f3 * (sSetup - bSetup);
				System.out.println(bBreak + " " + sSetup + " " + sEnter
						 + " " + bEnter + " " + bSetup + " " + sBreak);
			}
		//	System.out.println("barsUtil.highD(0) " + barsUtil.highD(0));
			if(sSetup == 0)
				return;
			if(getDirPositionVol(1) == 0 && getDirPositionVol(-1) == 0)
			{
				if(bar.getClose() > bBreak){
					buyToOpen(vol);
				}
				else if(bar.getClose() < sBreak){
					sellToClose(vol);
				}
			}else if(getDirPositionVol(1)!= 0){
				if(barsUtil.highD(0) > sSetup && bar.getClose() < sEnter)
					{
						System.out.println("barsUtil.highD(0) " + barsUtil.highD(0));
						sell(2 * getDirPositionVol(1));
					}
			}else if(getDirPositionVol(-1)!= 0){
				if(barsUtil.lowD(0) > bSetup && bar.getClose() > bEnter)
					{
					System.out.println("barsUtil.lowD(0) " + barsUtil.lowD(0));
						buy(2 * getDirPositionVol(-1));
					}
			}
	}
	
	
	
}
