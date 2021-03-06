package org.opencommercesearch.junit.runners;

/*
* Licensed to OpenCommerceSearch under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. OpenCommerceSearch licenses this
* file to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

import org.opencommercesearch.SearchServer;
import org.opencommercesearch.SearchServerManager;
import org.opencommercesearch.junit.SearchTest;
import org.opencommercesearch.junit.runners.statements.SearchInvokeMethod;
import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Implements a custom JUnit test case class model for search integration tests.
 *
 * @rmerizalde
 */
public class SearchJUnit4ClassRunner extends BlockJUnit4ClassRunner {

    public SearchJUnit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
    }

	/**
	 * Adds to {@code errors} for each method annotated with {@code @Test}that
	 * is not a public, void instance method with no arguments.
	 */
	protected void validateTestMethods(List<Throwable> errors) {
		validatePublicVoidNoArgMethods(Test.class, false, errors);
        validatePublicVoidMethods(SearchTest.class, false, errors);
	}

	/**
	 * Adds to {@code errors} if any method in this class is annotated with
	 * {@code annotation}, but:
	 * <ul>
	 * <li>is not public, or
	 * <li>takes parameters, or
	 * <li>returns something other than void, or
	 * <li>is static (given {@code isStatic is false}), or
	 * <li>is not static (given {@code isStatic is true}).
	 */
	protected void validatePublicVoidMethods(Class<? extends Annotation> annotation,
			boolean isStatic, List<Throwable> errors) {
		List<FrameworkMethod> methods= getTestClass().getAnnotatedMethods(annotation);

		for (FrameworkMethod eachTestMethod : methods)
			eachTestMethod.validatePublicVoid(isStatic, errors);
	}

	/**
	 * Returns the methods that run tests. Default implementation returns all
	 * methods annotated with {@code @Test} on this class and superclasses that
	 * are not overridden. In addition, it will return all methods annotated with
     * {@code @SearchTest}
     *
     * @todo validate SearchTest parameters
	 */
	protected List<FrameworkMethod> computeTestMethods() {
		List<FrameworkMethod> testMethods = super.computeTestMethods();
        List<FrameworkMethod> searchTestMethods = getTestClass().getAnnotatedMethods(SearchTest.class);
        List<FrameworkMethod> methods = new ArrayList<FrameworkMethod>(testMethods.size() + searchTestMethods.size());

        methods.addAll(testMethods);
        methods.addAll(searchTestMethods);

        return methods;
	}

	/**
	 * Returns a {@link org.junit.runners.model.Statement} that invokes {@code method} on {@code test}
	 */
	protected Statement methodInvoker(FrameworkMethod method, Object test) {
        SearchTest annotation = method.getAnnotation(SearchTest.class);
        if (annotation != null) {
            boolean newInstance = annotation.newInstance();
            boolean readOnly = !newInstance;

            SearchServerManager manager = SearchServerManager.getInstance();
            SearchServer server = null;
            Locale locale = new Locale(annotation.language());

            if (readOnly) {
                server = manager.getSearchServer();
            } else {
                String productDataResource = annotation.productData();
                String rulesDataResource = annotation.rulesData();
                String name = test.getClass().getName() + "_" + method.getName();

                server = manager.getSearchServerWithResources(name, productDataResource, rulesDataResource, locale);
            }

            return new SearchInvokeMethod(method, test, server);
        }
		return new InvokeMethod(method, test);
	}
}
