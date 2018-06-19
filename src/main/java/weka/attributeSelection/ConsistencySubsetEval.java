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
 *    ConsistencySubsetEval.java
 *    Copyright (C) 1999 Mark Hall
 *
 */

package  weka.attributeSelection;

import  java.io.*;
import  java.util.*;
import  weka.core.*;
import  weka.filters.supervised.attribute.Discretize;
import  weka.filters.Filter;

/** 
 * Consistency attribute subset evaluator. <p>
 *
 * For more information see: <br>
 * Liu, H., and Setiono, R., (1996). A probabilistic approach to feature 
 * selection - A filter solution. In 13th International Conference on 
 * Machine Learning (ICML'96), July 1996, pp. 319-327. Bari, Italy. 
 *
 * @author Mark Hall (mhall@cs.waikato.ac.nz)
 * @version $Revision: 1.10 $
 */
public class ConsistencySubsetEval extends SubsetEvaluator {
  
  /** training instances */
  private Instances m_trainInstances;

  /** class index */
  private int m_classIndex;

  /** number of attributes in the training data */
  private int m_numAttribs;

  /** number of instances in the training data */
  private int m_numInstances;

  /** Discretise numeric attributes */
  private Discretize m_disTransform;

  /** Hash table for evaluating feature subsets */
  private Hashtable m_table;

  /**
   * Class providing keys to the hash table.
   */
  public class hashKey implements Serializable {
    
    /** Array of attribute values for an instance */
    private double [] attributes;
    
    /** True for an index if the corresponding attribute value is missing. */
    private boolean [] missing;

    /** The values */
    private String [] values;

    /** The key */
    private int key;

    /**
     * Constructor for a hashKey
     *
     * @param t an instance from which to generate a key
     * @param numAtts the number of attributes
     */
    public hashKey(Instance t, int numAtts) throws Exception {

      int i;
      int cindex = t.classIndex();

      key = -999;
      attributes = new double [numAtts];
      missing = new boolean [numAtts];
      for (i=0;i<numAtts;i++) {
	if (i == cindex) {
	  missing[i] = true;
	} else {
	  if ((missing[i] = t.isMissing(i)) == false) {
	    attributes[i] = t.value(i);
	  }
	}
      }
    }

    /**
     * Convert a hash entry to a string
     *
     * @param t the set of instances
     * @param maxColWidth width to make the fields
     */
    public String toString(Instances t, int maxColWidth) {

      int i;
      int cindex = t.classIndex();
      StringBuffer text = new StringBuffer();
      
      for (i=0;i<attributes.length;i++) {
	if (i != cindex) {
	  if (missing[i]) {
	    text.append("?");
	    for (int j=0;j<maxColWidth;j++) {
	      text.append(" ");
	    }
	  } else {
	    String ss = t.attribute(i).value((int)attributes[i]);
	    StringBuffer sb = new StringBuffer(ss);
	    
	    for (int j=0;j < (maxColWidth-ss.length()+1); j++) {
		sb.append(" ");
	    }
	    text.append(sb);
	  }
	}
      }
      return text.toString();
    }

    /**
     * Constructor for a hashKey
     *
     * @param t an array of feature values
     */
    public hashKey(double [] t) {

      int i;
      int l = t.length;

      key = -999;
      attributes = new double [l];
      missing = new boolean [l];
      for (i=0;i<l;i++) {
	if (t[i] == Double.MAX_VALUE) {
	  missing[i] = true;
	} else {
	  missing[i] = false;
	  attributes[i] = t[i];
	}
      }
    }
    
    /**
     * Calculates a hash code
     *
     * @return the hash code as an integer
     */
    public int hashCode() {

      int hv = 0;
      
      if (key != -999)
	return key;
      for (int i=0;i<attributes.length;i++) {
	if (missing[i]) {
	  hv += (i*13);
	} else {
	  hv += (i * 5 * (attributes[i]+1));
	}
      }
      if (key == -999) {
	key = hv;
      }
      return hv;
    }

    /**
     * Tests if two instances are equal
     *
     * @param b a key to compare with
     */
    public boolean equals(Object b) {
      
      if ((b == null) || !(b.getClass().equals(this.getClass()))) {
        return false;
      }
      boolean ok = true;
      boolean l;
      if (b instanceof hashKey) {
	hashKey n = (hashKey)b;
	for (int i=0;i<attributes.length;i++) {
	  l = n.missing[i];
	  if (missing[i] || l) {
	    if ((missing[i] && !l) || (!missing[i] && l)) {
	      ok = false;
	      break;
	    }
	  } else {
	    if (attributes[i] != n.attributes[i]) {
	      ok = false;
	      break;
	    }
	  }
	}
      } else {
	return false;
      }
      return ok;
    }
    
    /**
     * Prints the hash code
     */
    public void print_hash_code() {
      
      System.out.println("Hash val: "+hashCode());
    }
  }

  /**
   * Returns a string describing this search method
   * @return a description of the search suitable for
   * displaying in the explorer/experimenter gui
   */
  public String globalInfo() {
    return "ConsistencySubsetEval :\n\nEvaluates the worth of a subset of "
      +"attributes by the level of consistency in the class values when the "
      +"training instances are projected onto the subset of attributes. "
      +"\n\nConsistency of any subset can never be lower than that of the "
      +"full set of attributes, hence the usual practice is to use this "
      +"subset evaluator in conjunction with a Random or Exhaustive search "
      +"which looks for the smallest subset with consistency equal to that "
      +"of the full set of attributes.\n";
  }

  /**
   * Constructor. Calls restOptions to set default options
   **/
  public ConsistencySubsetEval () {
    resetOptions();
  }

  /**
   * reset to defaults
   */
  private void resetOptions () {
    m_trainInstances = null;
  }

  /**
   * Generates a attribute evaluator. Has to initialize all fields of the 
   * evaluator that are not being set via options.
   *
   * @param data set of instances serving as training data 
   * @exception Exception if the evaluator has not been 
   * generated successfully
   */
  public void buildEvaluator (Instances data) throws Exception {
    if (data.checkForStringAttributes()) {
      throw  new UnsupportedAttributeTypeException("Can't handle string attributes!");
    }

    m_trainInstances = data;
    m_trainInstances.deleteWithMissingClass();
    m_classIndex = m_trainInstances.classIndex();
    if (m_classIndex < 0) {
      throw new Exception("Consistency subset evaluator requires a class "
			  + "attribute!");
    }

    if (m_trainInstances.classAttribute().isNumeric()) {
      throw new Exception("Consistency subset evaluator can't handle a "
			  +"numeric class attribute!");
    }

    m_numAttribs = m_trainInstances.numAttributes();
    m_numInstances = m_trainInstances.numInstances();

    m_disTransform = new Discretize();
    m_disTransform.setUseBetterEncoding(true);
    m_disTransform.setInputFormat(m_trainInstances);
    m_trainInstances = Filter.useFilter(m_trainInstances, m_disTransform);
  }

  /**
   * Evaluates a subset of attributes
   *
   * @param subset a bitset representing the attribute subset to be 
   * evaluated 
   * @exception Exception if the subset could not be evaluated
   */
  public double evaluateSubset (BitSet subset) throws Exception {
    int [] fs;
    int i;
    int count = 0;

    for (i=0;i<m_numAttribs;i++) {
      if (subset.get(i)) {
	count++;
      }
    }

    double [] instArray = new double[count];
    int index = 0;
    fs = new int[count];
    for (i=0;i<m_numAttribs;i++) {
      if (subset.get(i)) {
	fs[index++] = i;
      }
    }
    
    // create new hash table
    m_table = new Hashtable((int)(m_numInstances * 1.5));
    
    for (i=0;i<m_numInstances;i++) {
      Instance inst = m_trainInstances.instance(i);
      for (int j=0;j<fs.length;j++) {
	if (fs[j] == m_classIndex) {
	  throw new Exception("A subset should not contain the class!");
	}
	if (inst.isMissing(fs[j])) {
	  instArray[j] = Double.MAX_VALUE;
	} else {
	  instArray[j] = inst.value(fs[j]);
	}
      }
      insertIntoTable(inst, instArray);
    }

    return consistencyCount();
  }

  /**
   * calculates the level of consistency in a dataset using a subset of
   * features. The consistency of a hash table entry is the total number
   * of instances hashed to that location minus the number of instances in
   * the largest class hashed to that location. The total consistency is
   * 1.0 minus the sum of the individual consistencies divided by the
   * total number of instances.
   * @return the consistency of the hash table as a value between 0 and 1.
   */
  private double consistencyCount() {
    Enumeration e = m_table.keys();
    double [] classDist;
    double count = 0.0;
    
    while (e.hasMoreElements()) {
      hashKey tt = (hashKey)e.nextElement();
      classDist = (double []) m_table.get(tt);
      count += Utils.sum(classDist);
      int max = Utils.maxIndex(classDist);
      count -= classDist[max];
    }

    count /= (double)m_numInstances;
    return (1.0 - count);
  }

  /**
   * Inserts an instance into the hash table
   *
   * @param inst instance to be inserted
   * @param instA the instance to be inserted as an array of attribute
   * values.
   * @exception Exception if the instance can't be inserted
   */
  private void insertIntoTable(Instance inst, double [] instA)
       throws Exception {

    double [] tempClassDist2;
    double [] newDist;
    hashKey thekey;

    thekey = new hashKey(instA);

    // see if this one is already in the table
    tempClassDist2 = (double []) m_table.get(thekey);
    if (tempClassDist2 == null) {
      newDist = new double [m_trainInstances.classAttribute().numValues()];
      newDist[(int)inst.classValue()] = inst.weight();
      
      // add to the table
      m_table.put(thekey, newDist);
    } else { 
      // update the distribution for this instance
      tempClassDist2[(int)inst.classValue()]+=inst.weight();
      
      // update the table
      m_table.put(thekey, tempClassDist2);
    }
  }

  /**
   * returns a description of the evaluator
   * @return a description of the evaluator as a String.
   */
  public String toString() {
    StringBuffer text = new StringBuffer();

    if (m_trainInstances == null) {
      text.append("\tConsistency subset evaluator has not been built yet\n");
    }
    else {
      text.append("\tConsistency Subset Evaluator\n");
    }

    return text.toString();
  }

  /**
   * Main method for testing this class.
   *
   * @param args the options
   */
  public static void main (String[] args) {
    try {
      System.out.println(AttributeSelection.
			 SelectAttributes(new ConsistencySubsetEval(), args));
    }
    catch (Exception e) {
      e.printStackTrace();
      System.out.println(e.getMessage());
    }
  }
}
