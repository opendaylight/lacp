/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.lacp.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.opendaylight.lacp.core.LacpConst;
import org.opendaylight.lacp.core.LacpConst.BOND_TYPE;
import org.opendaylight.lacp.core.LagId;
import org.opendaylight.lacp.core.LagIdElem;

public class LacpAggregatorTest {
	LacpAggregator lag;
	
	@MockitoAnnotations.Mock
	private LacpBond lacpBond;
	
	@MockitoAnnotations.Mock
	private LacpAggregator lag1,lag2;
	
	private List<LacpPort> Portlist;
	
	@Before
    public void initMocks(){
        MockitoAnnotations.initMocks(this);
        lag = LacpAggregator.newInstance();
        Portlist=new ArrayList<LacpPort>();
        addPortsToList();
    }
	
	
	@Test
	public void SetterGetterTest(){
		SettersMethod(lag);
		GettersMethodTest(lag);
	}
	
	@Test
	public void AggregatorSelectionTest(){
		assertNull(LacpAggregator.aggregatorSelection(null, null));
		LacpAggregator lag1 = LacpAggregator.newInstance();
		LacpAggregator lag2 = LacpAggregator.newInstance();
		
		assertEquals(lag1,LacpAggregator.aggregatorSelection(lag1, null));
		assertEquals(lag2,LacpAggregator.aggregatorSelection(null, lag2));
		
		//Default return. When no case match
		//assertEquals(lag1,LacpAggregator.aggregatorSelection(lag1, lag2));

		//IsIndiv case
		lag1.setIsIndiv(false);
		lag2.setIsIndiv(true);
		assertEquals(lag1,LacpAggregator.aggregatorSelection(lag1, lag2));
		lag1.setIsIndiv(true);
		lag2.setIsIndiv(false);
		assertEquals(lag2,LacpAggregator.aggregatorSelection(lag1, lag2));
		
		lag1.setIsIndiv(true);
		lag2.setIsIndiv(true);
		

		//Partner Case

		byte[] partner= {0,0,0,0,0,0x02};
		lag2.setPartnerSystem(partner);
		partner[5]=0;
		lag1.setPartnerSystem(partner);
		assertEquals(lag2,LacpAggregator.aggregatorSelection(lag1, lag2));
		lag2.setPartnerSystem(partner);
		partner[5]=0x02;
		lag1.setPartnerSystem(partner);
		assertEquals(lag1,LacpAggregator.aggregatorSelection(lag1, lag2));
		
		lag2.setPartnerSystem(partner);
		
		
		//Bondtype switch case
		LacpBond bond = Mockito.mock(LacpBond.class);
		when(bond.getAggSelectionMode()).thenReturn(BOND_TYPE.BOND_COUNT);
		lag2.aggSetBond(bond);
		lag2.setNumOfPorts((short) 10); 
		lag1.setNumOfPorts((short) 5);
		assertEquals(lag2,LacpAggregator.aggregatorSelection(lag1, lag2));
		
		lag2.setNumOfPorts((short) 4); 
		lag1.setNumOfPorts((short) 12); 
		assertEquals(lag1,LacpAggregator.aggregatorSelection(lag1, lag2));
		lag2.setNumOfPorts((short) 6); 
		lag1.setNumOfPorts((short) 6); 
		
		//Bandwidthss
		LacpPort port1=Mockito.mock(LacpPort.class);
		LacpPort port2=Mockito.mock(LacpPort.class);
		when(bond.getAggSelectionMode()).thenReturn(BOND_TYPE.BOND_BANDWIDTH);
		when(port1.getLinkSpeed()).thenReturn((short) LacpConst.LINK_SPEED_BITMASK_1000MBPS);
		when(port2.getLinkSpeed()).thenReturn((short) LacpConst.LINK_SPEED_BITMASK_100MBPS);
		lag2.addPortToAgg(port1);
		lag1.addPortToAgg(port2);
		assertEquals(lag2,LacpAggregator.aggregatorSelection(lag1, lag2));
		

		when(bond.getAggSelectionMode()).thenReturn(BOND_TYPE.BOND_STABLE);
		when(port1.getLinkSpeed()).thenReturn((short) LacpConst.LINK_SPEED_BITMASK_10000MBPS);
		when(port2.getLinkSpeed()).thenReturn((short) LacpConst.LINK_SPEED_BITMASK_40000MBPS);
		assertEquals(lag1,LacpAggregator.aggregatorSelection(lag1, lag2));
		
		
	}
	
	@Test
	public void aggDevUpTest(){
		when(lag1.getLagPortList()).thenReturn(null);
		assertEquals(false,lag1.aggDevUp());
		lag.setLagPortList(Portlist);
		assertEquals(true,lag.aggDevUp());
	}
	
	@Test 
	public void AggPortsTest(){
		LacpPort port = Mockito.mock(LacpPort.class);
		lag.setLagPortList(null);
		assertEquals(false,lag.aggHasPort(port));
		lag.addPortToAgg(port);
		assertEquals(port, lag.getLastPortFromAgg());
		assertEquals(true,lag.aggHasPort(port));
		lag.rmPortFromAgg(port);
		assertNull(lag.getLastPortFromAgg());
		assertEquals(false,lag.aggHasPort(port));
		lag.rmPortFromAgg(port);
		//verify(lag,times(1)).clearAgg();
	}
	
	@Test
	public void AggStandByPortsTest(){
		LacpPort port = Mockito.mock(LacpPort.class);
		lag.setStandbyPorts(null);
		assertEquals(false,lag.aggHasStandbyPort(port));
		lag.addPortToAggStandBy(port);
		assertEquals(port, lag.getLastPortFromAggStandBy());
		assertEquals(true,lag.aggHasStandbyPort(port));
		lag.rmPortFromAggStandBy(port);
		assertNull(lag.getLastPortFromAggStandBy());
		assertEquals(false,lag.aggHasStandbyPort(port));
		lag.rmPortFromAggStandBy(port);
		//verify(lag,times(1)).getNumOfStandbyPort();
	}
	
	@Test
	public void PartnerTest(){
		byte[] partner= {0,0,0,0,0,0x02};
		assertEquals(false,lag.aggHasPartner());
		assertEquals(false,lag.aggPartnerIsNullMac());
		lag.setPartnerSystem(partner);
		assertEquals(true,lag.aggHasPartner());
		assertEquals(false,lag.aggPartnerIsNullMac());
		partner[5]=0;
		lag.setPartnerSystem(partner);
		assertEquals(false,lag.aggHasPartner());
		assertEquals(true,lag.aggPartnerIsNullMac());
	}
	
	@Test
	public void SelPortTest(){
		LacpPort port = Mockito.mock(LacpPort.class);
		LacpBond bond = Mockito.mock(LacpBond.class);
		LagId lagid = Mockito.mock(LagId.class);
		when(bond.bondGetMaxLink()).thenReturn(3);
		when(port.getActorPortNumber()).thenReturn((short) 8080);
		
		//Test for IsPortReachMaxCount(port)
		lag.aggSetBond(bond);
		
		Portlist.clear();
		Portlist.add(port);
		Portlist.add(port);
		addPortsToList();
		Portlist.add(port);
		lag.setLagPortList(Portlist);
		
		assertEquals(false,lag.IsPortReachMaxCount(null));
		
		
		assertEquals(true,lag.IsPortReachMaxCount(port));
		
		lag.rmPortFromAgg(lag.getLastPortFromAgg());

		assertEquals(false,lag.IsPortReachMaxCount(port));//End of test
		
		//ToDo:CanMoveToSelList
		lag.setStandbyPorts(Portlist);
		when(port.getStateMachineBitSet()).thenReturn((short)0)
										.thenReturn((short) 8080);
		when(port.portGetLagId()).thenReturn(lagid);
		lag.setAggLagId(lagid);
		
		assertEquals(false,lag.canMoveToSelList(null));
		assertEquals(false,lag.canMoveToSelList(port));
		assertEquals(true,lag.canMoveToSelList(port));
		
	}
	
	@Test
	public void ActionsWithPortsTest(){
		Portlist.clear();
		addPortsToList();
		for(LacpPort port: Portlist){
			if(Portlist.indexOf(port)==1){
				when(port.isPortAttDist()).thenReturn(true)
										.thenReturn(false);
				when(port.getPortsReady()).thenReturn(0)
										.thenReturn(1);
			}else{
				when(port.isPortAttDist()).thenReturn(false);
				when(port.getPortsReady()).thenReturn(1);
			}
		}
		lag.setLagPortList(Portlist);
		assertEquals(true,lag.existPortwithDist());
		assertEquals(false,lag.existPortwithDist());
		
		assertEquals(0,lag.getAggPortsReady());
		assertEquals(1,lag.getAggPortsReady());
		
		lag.setLagPortList(null);
		LagId aggLagId = Mockito.mock(LagId.class);
		lag.setAggLagId(aggLagId);
		LacpPort port = Mockito.mock(LacpPort.class);
		when(port.portGetLagId()).thenReturn(aggLagId);
		when(aggLagId.isNeighborFound()).thenReturn(true);
		lag.setIndiv(false);

		lag.setAggLagId(aggLagId);
		assertEquals(true,lag.isPortFitToAgg(port));
	}
	
	
	@Test
	public void SetAggBondTest(){
		LacpBond bond = Mockito.mock(LacpBond.class);
		byte[] address = {0x01,0x02,0x03,0x04,0x05,0x06};
		when(bond.getSysMacAddr()).thenReturn(address);
		lag.setAggBond(bond);
		byte[] newAgg=lag.getAggMacAddress();
		for(int i=0;i<LacpConst.ETH_ADDR_LEN;i++){
			assertEquals(address[i],newAgg[i]);
		}
	}
	
	@Test 
	public void copyAggfromOriginAggTest(){
		
		byte[] partner= {0,0,0,0,0,0x11};
		lag.setPartnerSystem(partner);
		LacpAggregator dest = LacpAggregator.newInstance();
		dest.setLagPortList(new ArrayList<LacpPort>());
		lag.setLagPortList(Portlist);
		LacpAggregator.copyAggfromOriginAgg(dest,lag);
		//GettersMethodTest(dest);
	}
	
	@Test
	public void copyAggInfoFromPortTest(){
		LacpPort port = Mockito.mock(LacpPort.class);
		LacpAggregator dest = LacpAggregator.newInstance();
		byte[] partSys = {0,0,0,0,0x12,0x15};
		byte[] elm1Mac = {0,0,0,0,0,0x50};
		byte[] elm2Mac = {0,0,0,0,0,0x60};
		LagIdElem elm1 = new LagIdElem(1, elm1Mac,(short)1001,20,(short)8085);
		LagIdElem elm2 = new LagIdElem(1, elm2Mac, (short)1005, 25, (short)8085);
		LagId lagid = new LagId(elm1, elm2);
		when(port.portPartnerOperGetSystem()).thenReturn(partSys);
		when(port.portGetLagId()).thenReturn(lagid);
		dest.copyAggInfoFromPort(port);
	}
	
	@Test
	public void compareToTest(){
		LagId lagid = Mockito.mock(LagId.class);
		when(lagid.compareTo(any(LagId.class))).thenReturn(-2).thenReturn(5);
		lag.setAggLagId(lagid);
		LacpAggregator lag2 = LacpAggregator.newInstance();
		lag2.setAggLagId(null);
		assertEquals(0,lag.compareTo(lag));
		assertEquals(-1,lag.compareTo(lag2));
		assertEquals(1,lag2.compareTo(lag));
		lag2.setAggLagId(Mockito.mock(LagId.class));
		when(lagid.compareTo(any(LagId.class))).thenReturn(-2).thenReturn(5);
		assertEquals(-2,lag.compareTo(lag2));
		assertEquals(5, lag.compareTo(lag2));
	}
	
	///TODO @Test for setAggrPortReady
	@Test
	public void testSetAggPortReady(){
		lag.setLagPortList(Portlist);
		 for (LacpPort port : lag.getLagPortList()){
	            doNothing().when(port).setPortsReady(any(Integer.class));
	        }
		lag.setAggPortsReady(1);
	}
	
	@Test
	public void GetterAndSetterTest(){
		lag.setLagPortList(Portlist);
		assertEquals(Portlist, lag.getLagPorts());
		
		lag.setReselect(true);
		assertEquals(true,lag.isReselect());
		
		lag.aggSetActorOperAggKey((short)1001);
		assertEquals((short)1001, lag.getActorOperAggKey());
		
		lag.setIsActive((short) 1);
		assertEquals((short) 1, lag.getIsActive());
		

		lag.setPartnerOperAggKey((short)11); 
		assertEquals((short) 11,lag.aggGetPartnerOperAggKey());
		
		lag.setIndiv(false);
		assertEquals(false,lag.getIsIndiv());
	}
	
	public void addPortsToList(){
		for(int i=0;i<3;i++){
			LacpPort p = Mockito.mock(LacpPort.class);
			//when(p.isInitialized).thenReturn(true);
			p.isInitialized=true;
			if(i==0){
				when(p.portGetPortStatus()).thenReturn(LacpConst.BOND_LINK_UP);
			}else if(i==1){
				when(p.portGetPortStatus()).thenReturn(LacpConst.BOND_LINK_BACK);
			}
			when(p.getActorPortNumber()).thenReturn((short) (2121+i));
			Portlist.add(p);
		}
	}
	
	public void SettersMethod(LacpAggregator lg){
		/*lg.setActorAdminAggregatorKey(key);
		lg.setActorOperAggKey(key);
		//lg.setAggBond(bond);
		lg.setAggId(aggId);
		lg.setAggLagId(aggLagId);
		lg.setAggMacAddress(aggMacAddress);
		lg.setAggPortsReady(val);
		lg.setBond(bond);
		//lg.setIndiv(isIndiv);
		lg.setIsActive(val);
		///lg.setIsIndiv(val);
		///lg.setLagPortList(PortList);
		///lg.setNumOfPorts(numOfPorts);
		lg.setNumOfStandbyPort(numOfStandbyPort);
		lg.setPartnerOperAggKey(key);
		///lg.setPartnerSystem(sys);
		lg.setPartnerSystemPriority(pri);
		lg.setReceiveState(receive_state);
		///lg.setStandbyPorts(standByPorts);
		lg.setTransmitState(transmit_state);
		lg.aggSetActorOperAggKey(key);
		lg.aggSetBond(bond);*/
	}
	public void GettersMethodTest(LacpAggregator lg){
		lg.getActorAdminAggregatorKey();
		lg.getActorOperAggKey();
		//lg.getAggBond();
		lg.getAggId();
		lg.getAggLagId();
		lg.getAggMacAddress();
		lg.getAggPortsReady();
		lg.getBond();
		//lg.getIndiv();
		lg.getIsActive();
		///lg.getIsIndiv();
		///lg.getLagPortList();
		///lg.getNumOfPorts();
		lg.getNumOfStandbyPort();
		lg.getPartnerOperAggKey();
		///lg.getPartnerSystem();
		lg.getPartnerSystemPriority();
		lg.getReceiveState();
		///lg.getStandByPorts();
		lg.getTransmitState();
		lg.aggGetActorOperAggKey();
		lg.aggGetBond();
	}
}
