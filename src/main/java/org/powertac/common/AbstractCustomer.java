/*
 * Copyright 2011-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an
 * "AS IS" BASIS,  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.powertac.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;
import org.powertac.common.enumerations.PowerType;
import org.powertac.common.interfaces.TariffMarket;
import org.powertac.common.repo.CustomerRepo;
import org.powertac.common.repo.RandomSeedRepo;
import org.powertac.common.repo.TariffRepo;
import org.powertac.common.repo.TariffSubscriptionRepo;
import org.powertac.common.repo.WeatherReportRepo;
import org.powertac.common.spring.SpringApplicationContext;

/**
 * Abstract customer implementation
 * 
 * @author Antonios Chrysopoulos
 */
public abstract class AbstractCustomer
{
  static protected Logger log = Logger.getLogger(AbstractCustomer.class
          .getName());

  protected String name = "dummy";
  protected HashMap<PowerType, List<CustomerInfo>> customerInfos;
  protected List<CustomerInfo> allCustomerInfos;

//  protected TariffMarket tariffMarketService;

//  protected CustomerRepo customerRepo;

  // Services available to subclasses, populated by setServices()
  protected WeatherReportRepo weatherReportRepo;
  protected RandomSeedRepo randomSeedRepo;
  protected TariffRepo tariffRepo;
  protected TariffSubscriptionRepo tariffSubscriptionRepo;

  /** The id of the Abstract Customer */
  protected long custId;

  /** The Customer specifications */
//  protected ArrayList<CustomerInfo> customerInfos;

  /** Random Number Generator */
  protected RandomSeed rs1;

  /**
   * Abstract Customer constructor. It takes the customerInfo as an input. It
   * creates the autowiring required using the Spring Application Context, it
   * creates the new Abstract Customer based on the customerInfo given, creates
   * a new random number generator and adds the newly created customer in the
   * CustomerRepo.
   */
  public AbstractCustomer (String name)
  {
    super();
//    randomSeedRepo =
//      (RandomSeedRepo) SpringApplicationContext.getBean("randomSeedRepo");
//    customerRepo =
//      (CustomerRepo) SpringApplicationContext.getBean("customerRepo");
//    tariffMarketService =
//      (TariffMarket) SpringApplicationContext.getBean("tariffMarketService");
//    tariffSubscriptionRepo =
//      (TariffSubscriptionRepo) SpringApplicationContext
//              .getBean("tariffSubscriptionRepo");

    custId = IdGenerator.createId();
    this.name = name;
    customerInfos = new HashMap<PowerType, List<CustomerInfo>>();
    allCustomerInfos = new ArrayList<CustomerInfo>();
  }

  /**
   * Populates the instance with service pointers to avoid Spring dependency.
   */
  public void setServices (RandomSeedRepo randomSeedRepo,
                           WeatherReportRepo weatherReportRepo,
                           TariffRepo tariffRepo,
                           TariffSubscriptionRepo tariffSubscriptionRepo)
  {
    this.randomSeedRepo = randomSeedRepo;
    this.weatherReportRepo = weatherReportRepo;
    this.tariffRepo = tariffRepo;
    this.tariffSubscriptionRepo = tariffSubscriptionRepo;
  }

  /**
   * Initializes the instance. Called after configuration, and after
   * a call to setServices().
   */
  public void initialize ()
  {
    rs1 = randomSeedRepo.getRandomSeed(name, 0, "TariffChooser");
  }

  /**
   * Adds an additional CustomerInfo to the list
   */
  public void addCustomerInfo (CustomerInfo info)
  {
    if (null == customerInfos.get(info.getPowerType())) {
      customerInfos.put(info.getPowerType(), new ArrayList<CustomerInfo>());
    }
    customerInfos.get(info.getPowerType()).add(info);
    allCustomerInfos.add(info);
  }

  /**
   * Returns the first CustomerInfo associated with this instance and PowerType. 
   * It is up to individual models to fill out the fields.
   */
  public CustomerInfo getCustomerInfo (PowerType pt)
  {
    return getCustomerInfoList(pt).get(0);
  }

  /**
   * Returns the list of CustomerInfos associated with this instance and
   * PowerType.
   */
  public List<CustomerInfo> getCustomerInfoList (PowerType pt)
  {
    return customerInfos.get(pt);
  }

  /**
   * Returns the list of CustomerInfo records associated with this customer
   * model.
   */
  public List<CustomerInfo> getCustomerInfos ()
  {
    return new ArrayList<CustomerInfo>(allCustomerInfos);
  }

  @Override
  public String toString ()
  {
    return Long.toString(getId()) + " " + getName();
  }

  public int getPopulation (CustomerInfo customer)
  {
    return customer.getPopulation();
    // JEC - what was this for??
    // return customerInfos.get(customerInfos.indexOf(customer)).getPopulation();
  }

  public long getCustId ()
  {
    return custId;
  }

  /** Synonym for getCustId() */
  public long getId ()
  {
    return custId;
  }

  public String getName ()
  {
    return name;
  }

  /**
   * Function utilized at the beginning in order to subscribe to the default
   * tariff
   * NOTE: This requires access to tariffMarketService, and is used only
   * during initialization (when the customerService could do the job) and
   * in test code. I recommend removal -- JEC.
   */
//  public void subscribeDefault ()
//  {
//    for (CustomerInfo customer: customerInfos) {
//
//      PowerType type = customer.getPowerType();
//      if (tariffMarketService.getDefaultTariff(type) == null) {
//        log.info("No default Subscription for type " + type.toString() + " of "
//                 + this.toString() + " to subscribe to.");
//      }
//      else {
//        tariffMarketService.subscribeToTariff(tariffMarketService
//                .getDefaultTariff(type), customer, customer.getPopulation());
//        log.info("CustomerInfo of type " + type.toString() + " of "
//                 + this.toString()
//                 + " was subscribed to the default broker successfully.");
//      }
//    }
//  }

  /**
   * Called to run the model forward one step.
   */
  public abstract void step ();

  /**
   * Called to evaluate tariffs.
   */
  public abstract void evaluateTariffs (List<Tariff> tariffs);

  // --------------------------------------------
  //   Test support only
  // --------------------------------------------

  private TariffMarket tariffMarketService;

  public void setTariffMarket (TariffMarket service)
  {
    tariffMarketService = service;
  }
  
  /**
   * In this overloaded implementation of the changing subscription function,
   * Here we just put the tariff we want to change and the whole population is
   * moved to another random tariff. NOTE: Used only for testing...
   * 
   * @param tariff
   */
  public void changeSubscription (Tariff tariff, Tariff newTariff,
                                  CustomerInfo customer)
  {
    TariffSubscription ts =
      tariffSubscriptionRepo.getSubscription(customer, tariff);
    int populationCount = ts.getCustomersCommitted();
    unsubscribe(ts, populationCount);
    subscribe(newTariff, populationCount, customer);
  }

  /** Subscribing a certain population amount to a certain subscription */
  public void subscribe (Tariff tariff,
                         int customerCount,
                         CustomerInfo customer)
  {
    tariffMarketService.subscribeToTariff(tariff, customer, customerCount);
    log.info(this.toString() + " " + tariff.getPowerType().toString() + ": "
             + customerCount + " were subscribed to tariff " + tariff.getId());

  }

  /** Unsubscribing a certain population amount from a certain subscription */
  public void unsubscribe (TariffSubscription subscription, int customerCount)
  {

    subscription.unsubscribe(customerCount);
    log.info(this.toString() + " "
             + subscription.getTariff().getPowerType().toString() + ": "
             + customerCount + " were unsubscribed from tariff "
             + subscription.getTariff().getId());

  }
}
