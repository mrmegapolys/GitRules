package ca.pfv.spmf.patterns.itemset_array_integers_with_count;

/* This file is copyright (c) 2008-2012 Philippe Fournier-Viger
* 
* This file is part of the SPMF DATA MINING SOFTWARE
* (http://www.philippe-fournier-viger.com/spmf).
* 
* SPMF is free software: you can redistribute it and/or modify it under the
* terms of the GNU General Public License as published by the Free Software
* Foundation, either version 3 of the License, or (at your option) any later
* version.
* SPMF is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU General Public License for more details.
* You should have received a copy of the GNU General Public License along with
* SPMF. If not, see <http://www.gnu.org/licenses/>.
*/


import java.util.ArrayList;
import java.util.List;
import com.megapolys.gitrules.spmf.Itemset;

/**
 * This class represents a set of itemsets, where an itemset is an array of integers 
 * with an associated support count. Itemsets are ordered by size. For
 * example, level 1 means itemsets of size 1 (that contains 1 item).
* 
 * @author Philippe Fournier-Viger
 */
public class Itemsets {
	/** We store the itemsets in a list named "levels".
	 Position i in "levels" contains the list of itemsets of size i */
	private final List<List<Itemset>> levels = new ArrayList<>();

	/**
	 * Constructor
	 */
	public Itemsets() {
		levels.add(new ArrayList<>()); // We create an empty level 0 by
												// default.
	}

	public void addItemset(Itemset itemset, int k) {
		while (levels.size() <= k) {
			levels.add(new ArrayList<>());
		}
		levels.get(k).add(itemset);
	}

	public int count() {
		int count = 0;
		for (List<Itemset> level : levels) {
			count += level.size();
		}
		return count;
	}

	public List<List<Itemset>> getLevels() {
		return levels;
	}
}
