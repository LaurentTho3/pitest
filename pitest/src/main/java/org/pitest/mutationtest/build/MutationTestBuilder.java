/*
 * Copyright 2011 Henry Coles
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and limitations under the License.
 */
package org.pitest.mutationtest.build;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.pitest.classinfo.ClassName;
import org.pitest.coverage.TestInfo;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationAnalyser;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.engine.MutationDetails;

public class MutationTestBuilder {

  private final MutationSource   mutationSource;
  private final MutationAnalyser analyser;
  private final WorkerFactory    workerFactory;
  private final MutationGrouper  grouper;

  public MutationTestBuilder(final WorkerFactory workerFactory,
      final MutationAnalyser analyser, final MutationSource mutationSource,
      final MutationGrouper grouper) {

    this.mutationSource = mutationSource;
    this.analyser = analyser;
    this.workerFactory = workerFactory;
    this.grouper = grouper;
  }

  public List<MutationAnalysisUnit> createMutationTestUnits(
      final Collection<ClassName> codeClasses) {
    final List<MutationAnalysisUnit> tus = new ArrayList<MutationAnalysisUnit>();

    final List<MutationDetails> mutations = FCollection.flatMap(codeClasses,
        classToMutations());
    
    Collections.sort(mutations, comparator());

    final Collection<MutationResult> analysedMutations = this.analyser
        .analyse(mutations);

    final Collection<MutationDetails> needAnalysis = FCollection.filter(
        analysedMutations, statusNotKnown()).map(resultToDetails());

    final List<MutationResult> analysed = FCollection.filter(analysedMutations,
        Prelude.not(statusNotKnown()));

    if (!analysed.isEmpty()) {
      tus.add(makePreAnalysedUnit(analysed));
    }

    if (!needAnalysis.isEmpty()) {
    	long start = System.currentTimeMillis();
        List<Integer> mutation_unit_size_list = new ArrayList<>();
      for (final Collection<MutationDetails> ms : this.grouper.groupMutations(
          codeClasses, needAnalysis)) {
    	  mutation_unit_size_list.add(ms.size());
//        tus.add(makeUnanalysedUnit(ms));
      }
//    Collections.sort(tus, new AnalysisPriorityComparator());
    
    	Map<String, List<MutationDetails>> method_mutation_map = new HashMap<>();
    	for (MutationDetails mutation : needAnalysis) {
    		String methodName = mutation.getClassName() + "#" + mutation.getMethod();
    		List<MutationDetails> mutation_list = method_mutation_map.get(methodName);
    		if (mutation_list == null) {
    			mutation_list = new ArrayList<>();
    		}
    		mutation_list.add(mutation);
    		method_mutation_map.put(methodName, mutation_list);
    	}
    	
    	for (String methodName : method_mutation_map.keySet()) {
    		List<MutationDetails> mutation_list = method_mutation_map.get(methodName);
    		Collections.shuffle(mutation_list);
    	}
    	
    	boolean remain = true;
    	Iterator<Integer> mutation_unit_size_iter = mutation_unit_size_list.iterator();
    	Integer mutation_unit_size = mutation_unit_size_iter.next();
		Collection<MutationDetails> method_based_mutation_list = new ArrayList<>();
    	do {
    		remain = false;
    		for (String methodName : method_mutation_map.keySet()) {
    			List<MutationDetails> mutation_list = method_mutation_map.get(methodName);
    			if (0 < mutation_list.size()) {
    				MutationDetails mutation = mutation_list.remove(0);
    				method_based_mutation_list.add(mutation);
    			}
    			if (method_based_mutation_list.size() == mutation_unit_size) {
    	    	    final Set<ClassName> uniqueTestClasses = new HashSet<ClassName>();
    	    	    FCollection.flatMapTo(method_based_mutation_list, mutationDetailsToTestClass(), uniqueTestClasses);
    	    	    MutationTestUnit mtu = new MutationTestUnit(method_based_mutation_list, uniqueTestClasses, this.workerFactory);
    	    		tus.add(mtu);

    	    		if (!mutation_unit_size_iter.hasNext()) {
    	    			break;
    	    		}
    	    		method_based_mutation_list = new ArrayList<>();
    	    		mutation_unit_size = mutation_unit_size_iter.next();
    			}
    			if (0 < mutation_list.size()) {
    				remain = true;
    			}
    		}
    	} while(remain);
    	long end = System.currentTimeMillis();
    	jp.mzw.adamu.adaptation.knowledge.Overhead.getInstance().insert(
    			jp.mzw.adamu.adaptation.knowledge.Overhead.Type.TestExecOrder, end - start);
    }
    
    return tus;
  }

  private Comparator<MutationDetails> comparator() {
    return new Comparator<MutationDetails>() {

      @Override
      public int compare(final MutationDetails arg0, final MutationDetails arg1) {
        return arg0.getId().compareTo(arg1.getId());
      }

    };
  }

  private F<ClassName, Iterable<MutationDetails>> classToMutations() {
    return new F<ClassName, Iterable<MutationDetails>>() {
      @Override
      public Iterable<MutationDetails> apply(final ClassName a) {
        return MutationTestBuilder.this.mutationSource.createMutations(a);
      }

    };
  }

  private MutationAnalysisUnit makePreAnalysedUnit(
      final List<MutationResult> analysed) {
    return new KnownStatusMutationTestUnit(analysed);
  }

  @SuppressWarnings("unused")
private MutationAnalysisUnit makeUnanalysedUnit(
      final Collection<MutationDetails> needAnalysis) {
    final Set<ClassName> uniqueTestClasses = new HashSet<ClassName>();
    FCollection.flatMapTo(needAnalysis, mutationDetailsToTestClass(),
        uniqueTestClasses);
    return new MutationTestUnit(needAnalysis, uniqueTestClasses,
        this.workerFactory);
  }

  private static F<MutationResult, MutationDetails> resultToDetails() {
    return new F<MutationResult, MutationDetails>() {
      @Override
      public MutationDetails apply(final MutationResult a) {
        return a.getDetails();
      }
    };
  }

  private static F<MutationResult, Boolean> statusNotKnown() {
    return new F<MutationResult, Boolean>() {
      @Override
      public Boolean apply(final MutationResult a) {
        return a.getStatus() == DetectionStatus.NOT_STARTED;
      }
    };
  }

  private static F<MutationDetails, Iterable<ClassName>> mutationDetailsToTestClass() {
    return new F<MutationDetails, Iterable<ClassName>>() {
      @Override
      public Iterable<ClassName> apply(final MutationDetails a) {
        return FCollection.map(a.getTestsInOrder(),
            TestInfo.toDefiningClassName());
      }
    };
  }

}
