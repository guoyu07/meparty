package com.teammental.memapper.core;

import com.teammental.memapper.configuration.MapConfigurationRegistry;

/**
 * Created by sa on 12/23/2017.
 */
public class MapConfigurationRegistrySingleton {

  private static MapConfigurationRegistry registry;

  /**
   * Returns the singleton instance of the registry.
   * @return registry
   */
  public static MapConfigurationRegistry getSingleton() {

    if (registry == null) {
      registry = new MapConfigurationRegistry();
    }
    return registry;
  }

  static void clean() {
    registry = null;
  }
}
