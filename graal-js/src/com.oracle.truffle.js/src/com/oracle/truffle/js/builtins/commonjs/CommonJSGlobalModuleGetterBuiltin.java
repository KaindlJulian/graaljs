/*
 * Copyright (c) 2020, 2022, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * The Universal Permissive License (UPL), Version 1.0
 *
 * Subject to the condition set forth below, permission is hereby granted to any
 * person obtaining a copy of this software, associated documentation and/or
 * data (collectively the "Software"), free of charge and under any and all
 * copyright rights in the Software, and any and all patent rights owned or
 * freely licensable by each licensor hereunder covering either (i) the
 * unmodified Software as contributed to or provided by such licensor, or (ii)
 * the Larger Works (as defined below), to deal in both
 *
 * (a) the Software, and
 *
 * (b) any piece of software and/or hardware listed in the lrgrwrks.txt file if
 * one is included with the Software each a "Larger Work" to which the Software
 * is contributed by such licensors),
 *
 * without restriction, including without limitation the rights to copy, create
 * derivative works of, display, perform, and distribute the Software and make,
 * use, sell, offer for sale, import, export, have made, and have sold the
 * Software and the Larger Work(s), and to sublicense the foregoing rights on
 * either these or other terms.
 *
 * This license is subject to the following condition:
 *
 * The above copyright notice and either this complete permission notice or at a
 * minimum a reference to the UPL must be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.oracle.truffle.js.builtins.commonjs;

import java.util.Map;

import com.oracle.truffle.api.CompilerDirectives;
import com.oracle.truffle.api.TruffleFile;
import com.oracle.truffle.api.dsl.Specialization;
import com.oracle.truffle.js.builtins.GlobalBuiltins;
import com.oracle.truffle.js.nodes.function.JSBuiltin;
import com.oracle.truffle.js.runtime.JSContext;
import com.oracle.truffle.js.runtime.JSRealm;
import com.oracle.truffle.js.runtime.Strings;
import com.oracle.truffle.js.runtime.builtins.JSOrdinary;
import com.oracle.truffle.js.runtime.objects.JSDynamicObject;
import com.oracle.truffle.js.runtime.objects.JSObject;

public abstract class CommonJSGlobalModuleGetterBuiltin extends GlobalBuiltins.JSFileLoadingOperation {

    CommonJSGlobalModuleGetterBuiltin(JSContext context, JSBuiltin builtin) {
        super(context, builtin);
    }

    @Specialization
    protected Object getObject() {
        return getOrCreateModuleObject(getContext(), getRealm());
    }

    @CompilerDirectives.TruffleBoundary
    public static JSDynamicObject getOrCreateModuleObject(JSContext context, JSRealm realm) {
        String filePath = CommonJSResolution.getCurrentFileNameFromStack();
        if (filePath != null) {
            TruffleFile truffleFile = realm.getEnv().getPublicTruffleFile(filePath);
            assert truffleFile.isRegularFile();
            Map<TruffleFile, JSDynamicObject> requireCache = realm.getCommonJSRequireCache();
            JSDynamicObject cached = requireCache.get(truffleFile);
            if (cached != null) {
                return cached;
            } else {
                JSDynamicObject moduleObject = createModuleObject(context, realm);
                requireCache.put(truffleFile, moduleObject);
                return moduleObject;
            }
        } else {
            return createModuleObject(context, realm);
        }
    }

    private static JSObject createModuleObject(JSContext context, JSRealm realm) {
        JSObject moduleObject = JSOrdinary.create(context, realm);
        JSObject exportsObject = JSOrdinary.create(context, realm);
        JSObject.set(moduleObject, Strings.EXPORTS_PROPERTY_NAME, exportsObject);
        return moduleObject;
    }

}
