/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 * Copyright (C) 2006 University of Waikato, Hamilton, New Zealand
 */

package weka.core;

import weka.test.WekaTestSuite;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test class for all core classes. Run from the command line with: <p/>
 * java weka.core.AllTests
 *
 * @author FracPete (frapcete at waikato dot ac dot nz)
 * @version $Revision: 6319 $
 */
public class AllTests
  extends WekaTestSuite {

  /**
   * generates all the tests
   *
   * @return		all the tests
   */
  public static Test suite() {
    TestSuite suite = new TestSuite();

    suite.addTest(InstancesTest.suite());
    suite.addTest(RangeTest.suite());
    suite.addTest(SingleIndexTest.suite());

    return suite;
  }

  /**
   * for running the tests from commandline
   *
   * @param args	the commandline arguments - ignored
   */
  public static void main(String []args) {
    junit.textui.TestRunner.run(suite());
  }
}
