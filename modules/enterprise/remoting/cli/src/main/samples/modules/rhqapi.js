/**
 * @overview this library tries to be synchronous and high-level API built on top of regular RHQ remote API. Compatible with JBoss ON 3.1.2 and 3.2.0
 * @name RHQ API
 * @version 0.3
 * @author Libor Zoubek (lzoubek@redhat.com), Filip Brychta (fbrychta@redhat.com), John Sanda (jsanda@redhat.com)
 *
 * 
 * If you want to contribute this code, please note following things
 * 
 * - do not return RHQ Domain objects out of rhqapi functions, always create a wrapper object which can hold and expose RHQ Domain object
 * - create namespace for each valid subsystem (resources, resourceTypes) and make sure that each subsystem has find() method that follows
 *   same criteria/query pattern as resources.find()
 * - If there is an asynchronous operation/method defined by RHQ remote API, make it synchronous and implement waiting
 * - enable logging 
 *   - make rhqapi log at TRACE level on every JS function you write
 *   - make rhqapi log at DEBUG level in case you think it might help understand what is going on
 *   - make rhqapi log at INFO level in case you need to inform user about some progress/status
 * - think of rhqapi the way it can be consumed interactively by human (if there are wrong inputs provided and there is a way to advise correct ones, do that)
 * - write JSDoc with examples
 * - don't forget to expose your subsystem/class to commonjs (look at the end of this file)
 * 
 */

// jsdoc-toolkit takes care of generating documentation
// please follow http://code.google.com/p/jsdoc-toolkit/wiki/TagReference for adding correct tag

/**
 * print function that recognizes arrays or JS objects (hashes) and prints each item on new line,
 * it produces JSON-like output (but NOT JSON)
 */
var p = function(object) {
	println(new _common().objToString(object))
};

//Production steps of ECMA-262, Edition 5, 15.4.4.19
//Reference: http://es5.github.com/#x15.4.4.19
if (!Array.prototype.map) {
	Array.prototype.map = function(callback, thisArg) {
		var T, A, k;
		if (this == null) {
			throw new TypeError(" this is null or not defined");
		}
		var O = Object(this);
		var len = O.length >>> 0;
		if (typeof callback !== "function") {
			throw new TypeError(callback + " is not a function");
		}
		if (thisArg) {
			T = thisArg;
		}
		A = new Array(len);
		k = 0;
		while (k < len) {
			var kValue, mappedValue;
			if (k in O) {
				kValue = O[k];
				mappedValue = callback.call(T, kValue, k, O);
				A[k] = mappedValue;
			}
			k++;
		}
		return A;
	};
}



/**
 * @ignore this common module is instantiated by most of modules as private var
 */
var _common = function() {
	var _println = function(object) {
		if (object instanceof Array) {
			object.forEach(function(x){println(x);});
		}
		else {
			println(object);
		}
	};
	var _time = function() {
		var now = new Date();
    var zeros = function(number) {if (number<10) { number = "0"+number;} return number;};
		return zeros(now.getHours())+ ":"+zeros(now.getMinutes())+":"+zeros(now.getSeconds());
	};

	var _trace = function(message) {
		if (typeof verbose == "number" && verbose>=2) {
			_println(_time()+" [TRACE] "+message);
		}
	};
	
	var _debug = function(message) {
		if (typeof verbose == "number" && verbose>=1) {
			_println(_time()+" [DEBUG] "+message);
		}
	};

	var _info = function(message) {
		if (typeof verbose == "number" && verbose>=0) {
			_println(_time()+" [INFO] "+message);
		}
	};
	var _warn = function(message) {
		if (typeof verbose == "number" && verbose>=-1) {
			_println(_time()+" [WARN] "+message);
		}
	};
	var _error = function(message) {
		if (typeof verbose == "number" && verbose>=-2) {
			_println(_time()+" [ERROR] "+message);
		}
	};
	// taken from CLI samples/utils.js
	/**
	 * A convenience function to convert javascript hashes into RHQ's
	 * configuration objects.
	 * <p>
	 * The conversion of individual keys in the hash follows these rules:
	 * <ol>
	 * <li> if a value of a key is a javascript array, it is interpreted as
	 * PropertyList
	 * <li> if a value is a hash, it is interpreted as a PropertyMap
	 * <li> otherwise it is interpreted as a PropertySimple
	 * <li> a null or undefined value is ignored
	 * </ol>
	 * <p>
	 * Note that the conversion isn't perfect, because the hash does not contain
	 * enough information to restore the names of the list members.
	 * <p>
	 * Example: <br/>
	 *
	 * <pre><code>
	 * {
	 * 	simple : &quot;value&quot;,
	 * 	list : [ &quot;value1&quot;, &quot;value2&quot; ],
	 * 	listOfMaps : [ {
	 * 		k1 : &quot;value&quot;,
	 * 		k2 : &quot;value&quot;
	 * 	}, {
	 * 		k1 : &quot;value2&quot;,
	 * 		k2 : &quot;value2&quot;
	 * 	} ]
	 * }
	 * </code></pre>
	 *
	 * gets converted to a configuration object: Configuration:
	 * <ul>
	 * <li> PropertySimple(name = "simple", value = "value")
	 * <li> PropertyList(name = "list")
	 * <ol>
	 * <li>PropertySimple(name = "list", value = "value1")
	 * <li>PropertySimple(name = "list", value = "value2")
	 * </ol>
	 * <li> PropertyList(name = "listOfMaps")
	 * <ol>
	 * <li> PropertyMap(name = "listOfMaps")
	 * <ul>
	 * <li>PropertySimple(name = "k1", value = "value")
	 * <li>PropertySimple(name = "k2", value = "value")
	 * </ul>
	 * <li> PropertyMap(name = "listOfMaps")
	 * <ul>
	 * <li>PropertySimple(name = "k1", value = "value2")
	 * <li>PropertySimple(name = "k2", value = "value2")
	 * </ul>
	 * </ol>
	 * </ul>
	 * Notice that the members of the list have the same name as the list itself
	 * which generally is not the case.
	 */
	var _asConfiguration = function(hash) {

		config = new Configuration();
		if (!hash) {
			return config;
		}
		for(_k in hash) {
			if (!hash.hasOwnProperty(_k)) {
				continue;
			}
			value = hash[_k];

			(function(parent, key, value) {
				function isArray(obj) {
					return typeof(obj) == 'object' && (obj instanceof Array);
				}

				function isHash(obj) {
					return typeof(obj) == 'object' && !(obj instanceof Array);
				}

				function isPrimitive(obj) {
					return typeof(obj) != 'object' || obj == null || (obj instanceof Boolean  || obj instanceof Number || obj instanceof String);
				}
				// this is an anonymous function, so the only way it can call
				// itself
				// is by getting its reference via argument.callee. Let's just
				// assign
				// a shorter name for it.
				var me = arguments.callee;

				var prop = null;

				if (isPrimitive(value)) {
					if (value==null) {
						prop = new PropertySimple(key, null);
					}
					else if (value instanceof Boolean) {
						prop = new PropertySimple(key, new java.lang.Boolean(value));
					}
					else if (value instanceof Number) {
						prop = new PropertySimple(key, new java.lang.Number(value));
					}
					else {
						prop = new PropertySimple(key, new java.lang.String(value));
					}
				} else if (isArray(value)) {
					prop = new PropertyList(key);
					for(var i = 0; i < value.length; ++i) {
						var v = value[i];
						if (v != null) {
							 me(prop, key, v);
						}
					}
				} else if (isHash(value)) {
					prop = new PropertyMap(key);
					for(var i in value) {
						var v = value[i];
						if (value != null) {
							 me(prop, i, v);
						}
					}
				}
				else {
					println("it is unkonwn");
					println(typeof value);
					println(value);
					return;
				}

				if (parent instanceof PropertyList) {
					parent.add(prop);
				} else {
					parent.put(prop);
				}
			})(config, _k, value);
		}

		return config;
	};

	// taken from CLI samples/utils.js
	/**
	 * Opposite of <code>asConfiguration</code>. Converts an RHQ's
	 * configuration object into a javascript hash.
	 *
	 * @param configuration
	 * @param configuration
	 *            definition - optional
	 */
	var _asHash = function(configuration,configDef) {
		ret = {};
		if (!configuration) {
			return ret;
		}
		iterator = configuration.getMap().values().iterator();
		while(iterator.hasNext()) {
			prop = iterator.next();
			var propDef;
			if (configDef) {
				if (configDef instanceof ConfigurationDefinition) {
					propDef = configDef.getPropertyDefinitions().get(prop.name);
				}
				else if (configDef instanceof PropertyDefinitionMap) {
					propDef = configDef.get(prop.name);
				}
			}
			(function(parent, prop) {
				function isArray(obj) {
					return typeof(obj) == 'object' && (obj instanceof Array);
				}

				function isHash(obj) {
					return typeof(obj) == 'object' && !(obj instanceof Array);
				}

				var me = arguments.callee;

				var representation = null;

				if (prop instanceof PropertySimple) {
					// we don't want to represent null values as string 'null'
					if(prop.stringValue != null){
						if (propDef && propDef instanceof PropertyDefinitionSimple) {
							// TODO implement all propertySimple types ..
							if (propDef.getType() == PropertySimpleType.BOOLEAN) {
	                            if (prop.booleanValue !=null) {
	                                if (prop.booleanValue == false) {
	                                	representation = false;
	                                }
	                                else {
	                                	representation = true;
	                                }
	                            }
	                        }
							else if (propDef.getType() == PropertySimpleType.DOUBLE
									|| propDef.getType() == PropertySimpleType.INTEGER
									|| propDef.getType() == PropertySimpleType.LONG
									|| propDef.getType() == PropertySimpleType.FLOAT
									) {
                                try {
                                    representation = Number(prop.doubleValue);

                                } catch (e) {
                                    _warn("Failed to type value "+prop+" as "+propDef.getType()+", this is a BUG in RHQ");
                                    representation = String(prop.stringValue);
                                }

							} else {
								representation = String(prop.stringValue);
							}
						}
						else {
							representation = String(prop.stringValue);
						}
					}
				} else if (prop instanceof PropertyList) {
					representation = [];

					for(var i = 0; i < prop.list.size(); ++i) {
						var child = prop.list.get(i);
						me(representation, child);
					}
				} else if (prop instanceof PropertyMap) {
					representation = {};

					var childIterator = prop.getMap().values().iterator();
					while(childIterator.hasNext()) {
						var child = childIterator.next();

						me(representation, child);
					}
				}

				if (isArray(parent)) {
					parent.push(representation);
				} else if (isHash(parent) && !prop.name.startsWith("__")) {
					parent[prop.name] = representation;
				}
			})(ret, prop);
		}
		(function(parent) {

		})(configuration);

		return ret;
	};

	/**
	 * applies map of values to given configuration
	 *
	 * @param original -
	 *            Configuration instance
	 * @param definition -
	 *            ConfigurationDefintion
	 * @param values -
	 *            map of values to be applied to configuration
	 *
	 * @return original Configuration object with applied values
	 */
	var _applyConfiguration = function(original,definition,values) {
		values = values || {};
		for (var _k in values) {
			// we only iterrate over values
			if (values.hasOwnProperty(_k)) {
				// parent - parent configuration
				// definition - parent configuration definition
				// key - config key to be applied
				// value - value to be applied
				(function (parent,definition,key,value) {
					var propDef = null;
					var prop = null;
					
					// decide which type of property are we working with
					if (definition instanceof PropertyDefinitionMap) {
						// println("DEF is map");
						propDef = definition.get(key);
					} else if (definition instanceof PropertyDefinitionList) {
						// println("DEF is list");
						propDef = definition.getMemberDefinition();
					} else if (definition instanceof ConfigurationDefinition) {
						// println("DEF is config");
						propDef = definition.getPropertyDefinitions().get(key);
					}

					// this handles cases when we have property map, and it's children do not have configDefinition
					// so there may be just key/values

					if (propDef==null && parent instanceof PropertyMap) {
						propDef = new PropertyDefinitionSimple(key,"",false,PropertySimpleType.STRING);
					}
					// ignore properties which don't have property definition (this is legal state)	// ignore properties which don't have property definition (this is legal state)
					if (propDef==null) {
						_warn("Unable to get PropertyDefinition for key="+key);
						return;
					}
					// process all 3 possible types
					if (propDef instanceof PropertyDefinitionSimple) {
						// _trace("applyConfiguration(), creating simple property");
						prop = new PropertySimple(key, null);

						if (value!=null) {
							if(value == 'null'){
								_warn("Adding property '"+ key +"' with null value as a string");
							}
							prop = new PropertySimple(key, new java.lang.String(value));
						}
					} else if (propDef instanceof PropertyDefinitionList) {
						// _trace("applyConfiguration(), creating list property");
						prop = new PropertyList(key);
						for(var i = 0; i < value.length; ++i) {
							arguments.callee(prop,propDef,"",value[i]);
						}
					} else if (propDef instanceof PropertyDefinitionMap) {
						// _trace("applyConfiguration(), creating map property");
						prop = new PropertyMap(propDef.name);
						for (var i in value) {
							if (value.hasOwnProperty(i)) {
								arguments.callee(prop,propDef,i,value[i]);
							}
						}
					}
					else {
						pretty.print(propDef);
						throw ("Unkonwn property definition! this is a bug, see above which property definition was passed");
					}
					// now we update our Configuration node
					if (parent instanceof PropertyList) {
						parent.add(prop);
					} else {
						parent.put(prop);
					}
				}) (original,definition,_k,values[_k]);
			}
		}

		return original;
	};
	
	var _objToString = function(hash) {
		function isArray(obj) {
			return typeof (obj) == 'object' && (obj instanceof Array);
		}
		function isHash(obj) {
			return typeof (obj) == 'object' && !(obj instanceof Array);
		}

		function isPrimitive(obj) {
			return typeof (obj) != 'object' || obj == null || obj instanceof Number || obj instanceof String || obj instanceof Boolean;
		}
		function isJavaObject(obj) {
			return typeof (obj) == 'object' && typeof (obj.getClass) != 'undefined'
		}
		if (!hash) {
			return hash;
		}
		// process only hashes, everything else is "just" string
		if (!isHash(hash)) {
			return String(hash);
		}
		output = "";
		for (_k in hash) {
			if (!hash.hasOwnProperty(_k)) {
				continue;
			}
			var valueStr = (function(key, value) {

				var me = arguments.callee;

				var prop = null;
				if (typeof value == "function") {
					return;
				}
				// if non-empty key was passed we are going to print this
				// element as key:<something>
				// otherwise there's no key to print
				var kkey = "";
				if (key != "") {
					kkey = key + ":";
				}
				if (isPrimitive(value)) {
					// put strings into quotes
					if (typeof value == "string" || value instanceof String) {
						prop = kkey + "\'" + value + "\'";
					} else {
						prop = kkey + value;
					}

				} else if (isJavaObject(value)) {
					// java object - should't be here
					prop = kkey + String(value);
				} else if (isArray(value)) {
					// by printing array we deeper (passing empty key)
					prop = kkey + "[";
					for ( var i = 0; i < value.length; ++i) {
						var v = value[i];
						if (v != null) {
							var repr = me("", v)
							if (repr) {
								// only if value was printed to something
								prop += repr + ",";
							}
						}
					}
					// trim last ','
					prop = prop.substring(0, prop.length - 1) + "]"
				} else if (isHash(value)) {
					// printing hash, again we go deeper
					prop = kkey + "{";
					for ( var i in value) {
						var v = value[i];
						var repr = me(i, v)
						if (repr) {
							prop += repr + ",";
						}
					}
					prop = prop.substring(0, prop.length - 1) + "}"
				} else {
					// this code should not be reached
					println("it is unkonwn");
					println(typeof value);
					println(value);
					return;
				}
				return prop;
			})(_k, hash[_k])

			if (valueStr) {
				output += valueStr + ",";
			}
		}
		output = output.substring(0, output.length - 1);
		return "{"+output+"}";
	}

	return {
		objToString : _objToString,
		arrayToSet : function(array){
			var hashSet = new java.util.HashSet();
			if(array){
				for(i in array){
					hashSet.add(array[i]);
				}
			}
			return hashSet;
		},
		pageListToArray : function(pageList) {
			var array = new Array();
		    var i = 0;
		    for(i = 0;i < pageList.size(); i++){
		    	array[i] = pageList.get(i);
		    }
		    return array;
		},
		/**
		 * @param conditionFunc -
		 *            predicate waits until conditionFunc does return any
		 *            defined value except for false
		 * @param funcDelay - delay (seconds) which overwrites globally defined delay
		 * @param funcTimeout - timeout (seconds) which overwrites globally defined timeout 
		 */
		waitFor : function(conditionFunc,funcDelay,funcTimeout) {
			var time = 0;
			
			var dlay = 5;
			if(funcDelay){
				dlay = funcDelay;
			}else if (typeof delay == "number") {
				dlay = delay;
			}
			
			var tout = 20;
			if(funcTimeout){
				tout = funcTimeout
			}else if (typeof timeout == "number") {
				tout = timeout;
			}
			
			_trace("common.waitFor(func,delay="+dlay+",timeout="+tout+")");

			var result = conditionFunc();
			while (time<tout && !result) {
				_debug("Waiting "+dlay+"s");
				sleep(dlay * 1000);
				time+=dlay;
				result = conditionFunc();
			}
			if (time>=tout) {
				_debug("Timeout "+tout+"s was reached!!");
			}
			return result;
		},
		error : _error,
		warn : _warn,
		info : _info,
		debug : _debug,
		trace : _trace,
		configurationAsHash : _asHash,
		hashAsConfiguration : _asConfiguration,
		applyConfiguration : _applyConfiguration,
		/**
		 * generic function to create any type of criteria. This function takes
		 * 'criteria' object and fills it with filter parameters given in
		 * 'param'. For param x in params call : criteria.addFilterX() will be
		 * done. There's also a shortcutFunction that can handle non-standart
		 * calls, for example function(key,value) { if (key=="status") {return
		 * "addFilterInventoryStatus.InventoryStatus"+value.toUpperCase()+")"}}
		 * so param key 'status' is processed by that returned string and this
		 * string is evaluated on the 'criteria' object
		 *
		 * @param {Criteria}
		 *            criteria - RHQ Criteria object
		 * @param {Object}
		 *            params - hash of parameters
		 * @param {function}
		 *            shortcutFunc - function(key,value), if returns string, it
		 *            will be evaluated
		 */
		createCriteria : function(criteria,params,shortcutFunc) {
			params = params || {};
			if (!criteria) {
				throw "Criteria object must be defined!";
			}
			_trace("Creating criteria with following params: " + _objToString(params));
			criteria.setStrict(true);
			for (var _k in params) {
			    // use hasOwnProperty to filter out keys from the
				// Object.prototype
			    if (params.hasOwnProperty(_k)) {
			    	if (_k=="_opts") {
			    		var _opts = params[_k]
			    		if (_opts.hasOwnProperty("strict") && typeof(_opts["strict"]) == "boolean") {
			    			criteria.setStrict(_opts["strict"]);
			    		}
			    		continue;
			    	}
			    	if (shortcutFunc) {
				    	var shortcutExpr = shortcutFunc(_k,params[_k]);
				    	if (shortcutExpr) {
				    		// shortcut func returned something so we can eval
							// it and skip normal processing for this property
				    		eval("criteria."+shortcutExpr);
				    		continue;
				    	}
			    	}
			        var __k = _k[0].toUpperCase()+_k.substring(1);
			        var func = eval("criteria.addFilter"+__k);
			        if (typeof func !== "undefined") {
			        	try {
			        		func.call(criteria,params[_k]);
			        	}
			        	catch (e) {
			        		throw "You have passed wrong argument (type="+typeof(params[_k])+") to filter "+_k + " "+ e;
			        	}
			        }
			        else {
			        	var names = "";
			        	criteria.getClass().getMethods().forEach( function (m) {
			        		if (m.getName().startsWith("addFilter")) {
			        		 var name = m.getName().substring(9);
			        		 names+=name.substring(0,1).toLowerCase()+name.substring(1)+", ";
			        		}
			        	});
			        	throw "Parameter ["+_k+"] is not valid filter parameter, valid filter parameters are : "+names;
			        }
			    }
			}
			
			return criteria;
		}
	};
};


/**
 * @namespace provides access to all available permissions
 */
var permissions = (function(){
	// get an array of native permissions
	var _globalPNat = Permission.GLOBAL_ALL.toArray();
	var _resourcePNat = Permission.RESOURCE_ALL.toArray() ;
	
	var _allGlobalP = new Array();
	var _allResourceP = new Array();
	
	// fill array with javascript strings 
	for(i in _globalPNat){
		// make sure we have javascript string
		_allGlobalP[i] = String(_globalPNat[i].toString());
	}
	for(i in _resourcePNat){
		// make sure we have javascript string
		_allResourceP[i] = String(_resourcePNat[i].toString());
	}
	
	
	var _allP = _allGlobalP.concat(_allResourceP); 

	return{
		/** 
		 * All available permission names
		 * @public
		 * @type String[]
		 * @returns Array of all permission names
		 */
		all : _allP,
		/** 
		 * All available global (global permissions do not apply to specific resources in groups) permission names
		 * @public
		 * @type String[]
		 */
		allGlobal : _allGlobalP,
		/** 
		 * All available resource (resource permissions apply only to the resources in the role's groups) permission names
		 * @public
		 * @type String[]
		 */
		allResource : _allResourceP,
		/** 
		 * Prints names and types of all permissions 
		 * @public
		 */
		printAllPermissions : function(){
			var perms = Permission.values();
			for(i in perms){
				println("Permission name: " + perms[i] + ", target: " + perms[i].getTarget());
			}
		}
	}
}) ();


//roles

/**
 * @namespace provides access to roles
 */
var roles = (function() {
	var common = new _common();
	
	// all valid accepted parameters
	var _validParams = ["description","name","permissions"];
	/** 
	 * Checks if given parameter is part of valid parameters, throw an error message otherwise 
	 * @private
	 * @param {string} param parameter to check
	 * @throws parameter is not valid
	 */
	var _checkParam = function(param){
		if(_validParams.indexOf(param) == -1){
        	throw "Parameter ["+param+"] is not valid, valid are : "+_validParams.valueOf();
		}
	};
	/** 
	 * Sets up given native Role according to given parameters 
	 * @private
	 * @param {org.rhq.core.domain.authz.Role} natRole native role to set up
	 * @param {Object} params
	 * @returns {org.rhq.core.domain.authz.Role} prepared native role
	 * @throws some of given parameters are not valid
	 */
	var _setUpNatRole = function(natRole,params){
		for (var k in params) {
		    // use hasOwnProperty to filter out keys from the
			// Object.prototype
		    if (params.hasOwnProperty(k)) {
		    	_checkParam(k);
		        var _k = k[0].toUpperCase()+k.substring(1);
	        	var func = eval("natRole.set"+_k);
	        	if(typeof func == "undefined"){
		        	throw "Given parameter '"+_k+"' is not defined on org.rhq.core.domain.authz.Role object";
		        }
	        	func.call(natRole,params[k]);
		    }
		}
		
		return natRole;
	};
	
	var _findRoles = function(params){
		common.debug("Searching for roles '"+common.objToString(params) +"'");
		var criteria = common.createCriteria(new RoleCriteria(),params);
		criteria.clearPaging();
		var roles = RoleManager.findRolesByCriteria(criteria);

		return common.pageListToArray(roles).map(function(x){return new Role(x);});		
	}
	
	var _getRole = function(roleName){
		common.debug("Searching for role with name '"+roleName +"'");
		var roles = _findRoles({name:roleName});

		for(i in roles){
			if(roles[i].name == roleName){
				common.debug("Role " + roleName+ " found.");
				return roles[i];
			}
		}
		
		common.debug("Role " + roleName+ " not found.");
		return null;
	}
	
	return {
		/** 
		 * Creates a new role acorrding to given parameters.
		 * @public
		 * @param {Object} params - see roles.validParams for available params.
		 * @example roles.createRole({name: "boss",description:"Role with all permissions.",permissions:permissions.all });
		 * @returns {Role} a newly created role
		 */
		createRole : function(params){
			params = params || {};
			common.info("Creating a new role with following parameters: '"+common.objToString(params) +"'");
			
			// check permission names and prepare hash set with native permissions
			if(params.permissions){
				var permSet = new java.util.HashSet();
				for(i in params.permissions){
					try
					{
						permSet.add(Permission.valueOf(params.permissions[i]));
					} catch (exc) {
						throw "'" + params.permissions[i]+"', permission name is not correct!";
					}
				}
				params.permissions = permSet;
			}
			
			var rawRole = _setUpNatRole(new org.rhq.core.domain.authz.Role,params);
			var role = RoleManager.createRole(rawRole);

			return new Role(role);
		},
		/** 
		 * Deletes given roles.
		 * @public
		 * @param {Array} roleNames - array with names of roles to delete
		 * @example roles.deleteRoles(["boss","guest"]);
		 */
		deleteRoles : function(roleNames){
			if(typeof roleNames == 'string'){
				roleNames = [roleNames]
			}
			common.info("Removing roles with following names: '"+common.objToString(roleNames) +"'");
			var role;
			for(i in roleNames){
				role = _getRole(roleNames[i]);
				if(role){
					RoleManager.deleteRoles([role.id]);
				}
			}
		},
		/** 
		 * Gets a given role. Returns found role or null.
		 * @public
		 * @function
		 * @param {string} roleName - name of the role
		 * @returns {Role} a found role or null
		 */
		getRole : _getRole,
		/** 
		 * Finds all roles according to given parameters.
		 * @public
		 * @function
		 * @param {Object} params -  see RoleCriteria.addFilter[param] methods for available params
		 * @returns  Array of found roles.
		 * @type Role[]
		 */
		findRoles : _findRoles,
		/**
		 * Prints all valid accepted parameters
		 * @public
		 */
		printValidParams : function(){
			println("Parameter names: " + _validParams.valueOf());
		}
	}
	
}) ();

/**
 * Creates a new instance of Role
 * @class
 * @constructor
 * @param nativeRole {org.rhq.core.domain.authz.Role} native role
 */
var Role = function(nativeRole){
	var common = new _common();
	common.debug("Creating an abstract role: " + nativeRole);
	if (!nativeRole) {
		throw "org.rhq.core.domain.authz.Role parameter is required";
	}
	
	var _nativeRole = nativeRole;
	var _name = _nativeRole.getName();
	var _id = _nativeRole.getId();
	
	/**
	 * @lends Role.prototype
	 */
	return{
		/**
		 * Name of this role
		 * @public
		 * @field
		 */
		name : _name,
		/**
		 * Id of this role
		 * @public
		 * @field
		 */
		id : _id,
		/**
		 * Native role which this object abstracts.
		 * @public
		 * @field
		 * @type org.rhq.core.domain.authz.Role
		 */
		nativeObj : _nativeRole,
		/**
		 * Returns array of all permissions this Role has.
		 * @public
		 * @returns {Array}
		 */
		getPermissions : function(){
			var permissSet = _nativeRole.getPermissions();
			
			return permissSet.toArray();
		},
	      /**
         * assigns given resource groups to this role. Note that this cleans up all previously assigned groups
         * @param {BundleGroup[]} groupArray - resource groups to be assigned with this role
         */
        assignBundleGroups : function(groupArray) {
            groupArray = groupArray || [];
            RoleManager.setAssignedBundleGroups(_id,groupArray.map(function(g){return g.id;}))
        },
		/**
		 * Returns array of BundleGroups assigned to this role
		 * @return {BundleGroup[]}
		 */
		bundleGroups : function() {
		    // TODO implement
		   return bundleGroups.find({roleIds:[_id]});
		},
		/**
		 * assigns given resource groups to this role. Note that this cleans up all previously assigned groups
		 * @param {ResGroup[]} groupArray - resource groups to be assigned with this role
		 */
		assignResourceGroups : function(groupArray) {
		    groupArray = groupArray || [];
		    RoleManager.setAssignedResourceGroups(_id,groupArray.map(function(g){return g.id;}))
		},
		/**
		 * Returns array of ResourceGroups assigned to this role
		 * @return {ResGroup[]}
		 */
		resourceGroups : function() {
		    var _groups = ResourceGroupManager.findResourceGroupsForRole(_id,PageControl.getUnlimitedInstance());
		    return common.pageListToArray(_groups).map(function(g) {return new ResGroup(g)});
		}
	}
};

// users

/**
 * @namespace provides access to users
 */
var users = (function() {
	var common = new _common();
	
	// all valid accepted parameters
	var _validParams = ["department","emailAddress","factive","firstName","lastName","name","phoneNumber","roles"];
	/** 
	 * Checks if given parameter is part of valid parameters, throw an error message otherwise 
	 * @private
	 * @param {string} param parameter to check
	 * @throws parameter is not valid
	 */
	var _checkParam = function(param){
		if(_validParams.indexOf(param) == -1){
        	throw "Parameter ["+param+"] is not valid, valid are : "+_validParams.valueOf();
		}
	};
	/** 
	 * Sets up given native Subject according to given parameters 
	 * @private
	 * @param {org.rhq.core.domain.auth.Subject} subject native subject to set up
	 * @param {Object} params
	 * @returns {org.rhq.core.domain.auth.Subject} prepared native subject
	 * @throws some of given parameters are not valid
	 */
	var _setUpSubject = function(subject,params){
		for (var k in params) {
		    // use hasOwnProperty to filter out keys from the
			// Object.prototype
		    if (params.hasOwnProperty(k)) {
		    	_checkParam(k);
		        var _k = k[0].toUpperCase()+k.substring(1);
		        var func = eval("subject.set"+_k);
		        if(typeof func == "undefined"){
		        	throw "Given parameter '"+_k+"' is not defined on Subject object";
		        }
		        func.call(subject,params[k]);
		    }
		}
		
		return subject;
	}
	var _findUsers = function(params){
		common.debug("Searching for users with following params: '"+common.objToString(params) +"'");
		var criteria = common.createCriteria(new SubjectCriteria(),params);
		criteria.clearPaging();
		var users = SubjectManager.findSubjectsByCriteria(criteria);
		
		return common.pageListToArray(users).map(function(x){return new User(x);});
	}
	
	var _getUser = function(userName){
		common.debug("Searching for user with name: '"+userName +"'");
		var users = _findUsers({name:userName});

		for(i in users){
			if(users[i].name == userName){
				common.debug("User " + userName+ " found.");
				return users[i];
			}
		}
		
		common.debug("User " + userName+ " not found.");
		return null;
	}
	
	return {
		/** 
		 * Creates a new user acorrding to given parameters.
		 * @public
		 * @param {Object} params - see users.validParams for available params.
		 * @param {string} password 
		 * @example users.addUser({firstName:"John",lastName:"Rambo",name:"jrambo",password:"passw",roles:["boss","admin"]);
		 * @returns {User} a newly created user
		 */
		addUser : function(params,password){
			params = params || {};
			common.info("Adding following user: '"+common.objToString(params) +"'");
			if(!password){
				throw ("Password is expected for a new user.");
			}
			
			var roleNames = params.roles;
			// fill it with native type, given roles will be added later using RoleManager
			params.roles = new java.util.HashSet();
		
			var rawSubject = _setUpSubject(new Subject,params);
			var subject = SubjectManager.createSubject(rawSubject);
			SubjectManager.createPrincipal(subject.getName(),password);
			
			var user = new User(subject);
			if(roleNames){
				user.assignRoles(roleNames);
			}

			return user;
		},
		/** 
		 * Deletes given user.
		 * @public
		 * @param {String[]} userNames - array with names of users to delete
		 * @example users.deleteUsers(["jrambo"]);
		 */
		deleteUsers : function(userNames){
			if(typeof userNames == 'string'){
				userNames = [userNames]
			}
			common.info("Removing users with following names: '"+common.objToString(userNames) +"'");
			
			var user;
			for(i in userNames){
				user = _getUser(userNames[i]);
				if(user){
					SubjectManager.deleteSubjects([user.id]);
				}
			}
		},
        /**
         * Finds all users according to given parameters.
         * @public
         * @function
         * @param {Object} params -  see SubjectCriteria.addFilter[param] methods for available params
         * @returns  Array of found users.
         * @type Users[]
         */
        find : _findUsers,
        /**
		 * Finds all users according to given parameters. (deprecated)
		 * @public
		 * @function
		 * @param {Object} params -  see SubjectCriteria.addFilter[param] methods for available params
		 * @returns  Array of found users.
		 * @type Users[]
		 */
		findUsers : _findUsers,
		/** 
		 * Gets a given user. Returns found user or null.
		 * @public
		 * @function
		 * @param {string} userName - name of the user
		 * @returns {User} a found user or null
		 */
		getUser : _getUser,
		/**
		 * Gets all available users.
		 * @public
		 * @function
		 * @returns  Array of found users.
		 * @type User[]
		 */
		getAllUsers : function(){
			common.debug("Gettign all users");

			return _findUsers({});
		},
		/**
		 * Prints all valid accepted parameters
		 * @public
		 */
		printValidParams : function(){
			println("Parameter names: " + _validParams.valueOf());
		}
		
	}
	
}) ();

/**
 * Creates a new instance of User
 * @class
 * @constructor
 * @param nativeRole {org.rhq.core.domain.authz.Subject} native subject
 */
var User = function(nativeSubject){
	var common = new _common();
	common.debug("Creating following abstract user: " + nativeSubject );
	if (!nativeSubject) {
		throw "org.rhq.core.domain.authz.Subject parameter is required";
	}
	
	var _id = nativeSubject.getId();
	var _name = nativeSubject.getName();
	
	/**
	 * @lends User.prototype
	 */
	return{
		/**
		 * Id of this user
		 * @public
		 * @field
		 */
		id : _id,
		/**
		 * Name of this user
		 * @public
		 * @field
		 */
		name : _name,
		/**
		 * Native subject which this object abstracts.
		 * @public
		 * @field
		 * @type org.rhq.core.domain.authz.Subject
		 */
		nativeObj : nativeSubject,
		/**
		 * Gets all roles assigned to this user.
		 * @public
		 * @returns  Array of found roles.
		 * @type Role[]
		 */
		getAllAssignedRoles : function(){
			common.debug("Searching for assigned roles to user '"+_name+"'");
			var natRoles = RoleManager.findSubjectAssignedRoles(_id,PageControl.getUnlimitedInstance());
			
			return common.pageListToArray(natRoles).map(function(x){return new Role(x);});
		},
		/**
		 * Assigns given roles to this user.
		 * @public 
		 * @param {String[]} roleNames array of names of roles which will be assigned to this user
		 */
		assignRoles : function(roleNames){
			common.info("Assigning following roles '"+ common.objToString(roleNames) +"', to user '"+_name+"'");

			if(typeof roleNames == 'string'){
				roleNames = [roleNames];
			}
			var rolesIds = new Array();
			var j = 0;
			var role;
			for(i in roleNames){
				role = roles.getRole(roleNames[i]);
				if(role){
					common.debug("Adding found role " + role);
					rolesIds[j] = role.id;
					j++;
				}else{
					common.info("Role " + roleNames[i]+ " not found!!");
				}
			}
			RoleManager.addRolesToSubject(_id,rolesIds);
		}
	}
};
// resource groups

/**
 * @namespace provides access to Resource groups
 */
var groups = (function() {
	var common = new _common();

	return {
		/**
		 * creates a org.rhq.domain.criteria.ResourceGroupCriteria object based on
		 * given params
		 *
		 * @param {Obejct}
		 *            params - criteria params
		 * @returns ResourceGroupCriteria
		 * @ignore
		 */
		createCriteria : function(params) {
			params = params || {};
			common.debug("groups.createCriteria("+common.objToString(params) +")");
			var criteria = common.createCriteria(new ResourceGroupCriteria(),params, function (key,value) {
				if (key=="category") { return "addFilterExplicitResourceCategory(ResourceCategory."+value.toUpperCase();}
			});
			criteria.clearPaging();
			return criteria;
		},
		/**
		 * finds resource groups by given params
		 * @param {Object} params
		 * see ResourceGroupCriteria for available params
		 * There are also shortcuts for ENUM parameters: - you can use
		 * <ul>
		 *    <li>{category:"platform"} insetead of {explicitResourceCategory:"ExplicitResourceCategory.PLATFORM"}</li>
		 *  </ul>
		 * @type ResGroup[]
		 */
		find : function(params) {
			params = params || {};
			common.debug("groups.find("+common.objToString(params)+")");
			var criteria = groups.createCriteria(params);
			var result = ResourceGroupManager.findResourceGroupsByCriteria(criteria);
			common.debug("Found "+result.size()+" groups ");
		    return common.pageListToArray(result).map(function(x){return new ResGroup(x);});
		},
    /**
	 * creates a new resource group. If all children are same type, COMPATIBLE
	 * group is created
	 *
	 * @param {String} name for a new group
	 * @param {Resource[]} children - array of resources that represents content of this group
	 * @type ResGroup
	 */
		create : function(name,children) {
			children = children || [];
			common.info("Creating a group '" + name + "', with following children: '" + common.objToString(children) + "'");
			var rg = new ResourceGroup(name);
			// detect whether all resources are same type
			var resType = null;
			var mixed = false;
			children.forEach(function(x) {
				if (resType==null) {
					resType = x.getProxy().resourceType;
				} else if (resType.id != x.getProxy().resourceType.id){
					mixed = true;
				}
			});

			if (resType!=null && !mixed) {
				common.debug("All given resources are ["+resType+"] type, creating compatible group");
				rg.setResourceType(resType);
			}
			var group = ResourceGroupManager.createResourceGroup(rg);
			ResourceGroupManager.addResourcesToGroup(group.id,children.map(function(x){return x.getId();}));
			return new ResGroup(group);
		}
	};
}) ();
/**
 * @class
 * @constructor
 */
var ResGroup = function(param) {
	var common = new _common();
	common.debug("new ResGroup("+param+")");
	if (!param) {
		throw "either number or org.rhq.core.domain.resource.ResourceGroup parameter is required";
	}
	var _id = param.id;
	var _obj = param;
	var _name = param.name;
	var _getCategory = function(){
		return _obj.getGroupCategory();
	}
	var _resources = function(params){
		params = params || {};
		params.explicitGroupIds = [_id];
		return resources.find(params);
	}
	var _resourcesImpl = function(params){
		params = params || {};
		params.implicitGroupIds = [_id];
		return resources.find(params);
	}
	var _waitForOperationResult = function(groupOpShedule) {
		var opHistCriteria = new GroupOperationHistoryCriteria();
		if (groupOpShedule)
			opHistCriteria.addFilterJobId(groupOpShedule.getJobId());
		var list = new java.util.ArrayList();
		list.add(new java.lang.Integer(_id));
		opHistCriteria.addFilterResourceGroupIds(list);
		opHistCriteria.addSortStartTime(PageOrdering.DESC); // put most recent
		// at top of results
		opHistCriteria.clearPaging();
		var pred = function() {
			var histories = OperationManager
					.findGroupOperationHistoriesByCriteria(opHistCriteria);
			if (histories.size() > 0) {
				if (histories.get(0).getStatus() != OperationRequestStatus.INPROGRESS) {
					return histories.get(0);
				}
				common.debug("Operation in progress..");
			}
			;
		};
		common.debug("Waiting for result..");
		sleep(3000); // trying to workaround
						// https://bugzilla.redhat.com/show_bug.cgi?id=855674
		var history = common.waitFor(pred);
		if (!history) {
			// timed out
			var histories = OperationManager
					.findGroupOperationHistoriesByCriteria(opHistCriteria);
			if (histories.size() > 0) {
				history = histories.get(0);
			} else {
				throw "ERROR Cannot get operation history result remote API ERROR?";
			}
		}
		common.debug("Operation finished with status : " + history.status);
		return history;
	};
	var _scheduleOperation = function(name,delay,repeatInterval,repeatCount,timeout,
			haltOnFailure,executionOrderResourceIds,description,opParams){
		delay = delay || 0;
		repeatInterval = repeatInterval || 0;
		repeatCount = repeatCount || 0;
		timeout = timeout || 0;
		haltOnFailure = haltOnFailure || true;
		executionOrderResourceIds = executionOrderResourceIds || null;
		description = description || null;
		
		common.trace("Group(" + _id + ")._scheduleOperation(name="
				+ name + ", delay=" + delay + ", repeatInterval="+repeatInterval 
				+ ", repeatCount=" + repeatCount 
				+", timeout="+timeout
				+", haltOnFailure="+haltOnFailure
				+", executionOrderResourceIds="+common.objToString(executionOrderResourceIds)
				+", description="+description
				+", opParams="+common.objToString(opParams)+")");
		
		if (_getCategory() == GroupCategory.MIXED) {
			throw "It's not possible to invoke operation on MIXED group!!"
		}

		// get resources in this group
		var groupRes = _resources();
		var res = groupRes[0];
		// check that operation is correct and get operation configuration
		var conf = res.checkOperation(name, opParams);
		
		var opShedule = OperationManager.scheduleGroupOperation(
				_id, executionOrderResourceIds, haltOnFailure, name, common.hashAsConfiguration(conf),
				delay * 1000, repeatInterval * 1000, repeatCount,timeout,description)
		common.info("Group operation '" + name + "' scheduled on '" + _name
				+ "'");
		
		return opShedule;
	};
	var _getMetricSchedules = function(metricName){
		var criteria = common.createCriteria(new MeasurementDefinitionCriteria(),
				{resourceTypeId:_resources()[0].getProxy().resourceType.id,displayName:metricName});				
		var mDefs = MeasurementDefinitionManager.findMeasurementDefinitionsByCriteria(criteria);
		
		var index = -1
		for (i=0;i<mDefs.size();i++) {
			if (mDefs.get(i).displayName == metricName) {
				index = i;
				break;
			}
		}
		if (index == -1) {
			throw "Unable to retrieve measurement definition with following name: " + metricName;
		}
		var mDefId = mDefs.get(index).id;
		
		common.trace("Retreaving schedules for goup with id: " +_id + 
				" and measurement definition id: " +mDefId);
		var criteria = common.createCriteria(new MeasurementScheduleCriteria(),
				{resourceGroupId:_id,definitionIds:[mDefId]});				
		var schedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);
		
		if(schedules.size()==0){
			throw "Unable to retrive schedule for this Metric!!";
		}
		
		return schedules;
	};
    /**
	 * @lends ResGroup.prototype
	 */
	return {
		/**
		 * gets ID of this group
		 * @field
		 */
		id : _id,
		/**
		 * gets name of this group
		 * @field
		 */
		name : _name,
		/**
		 * gets underlying ResourceGroup instance
		 * @field
		 *
		 */
		obj : _obj,
		/**
		 * returns ID of this group
		 * @function
		 * @type Number
		 */
		getId : function() {return _id;},
		/**
		 * removes this resource group
		 */
		remove : function() {
			common.info("Removing a group with name '" + _name + "'");
			ResourceGroupManager.deleteResourceGroup(_id);
		},
		/**
		 * get explicit resources contained in this group
		 * @param params - you can filter child resources same way as in {@link resources.find()} function
		 * @returns array of explicit resources
		 * @type Resrouce[]
		 * @function
		 */
		resources : _resources,
		/**
		 * get implicit resources contained in this group
		 * @param params - you can filter child resources same way as in {@link resources.find()} function
		 * @returns array of implicit resources
		 * @type Resrouce[]
		 * @function
		 */
		resourcesImpl : _resourcesImpl,
		/**
		 * schedules operation on this group using cron expression. In contrast to runOperation this is
		 * not blocking operation.
		 *
		 * @param {String}
		 *            name of operation (required)
		 *            
		 * @param {String} cronExpression (required)
		 * @param {Object}
		 *            opParams - hashmap for operation params (Configuration) (optional)
		 * @example allAgents.scheduleOperationUsingCron("executeAvailabilityScan","5 5 5 * * ?",{changesOnly:false});
		 */
		scheduleOperationUsingCron : function(name,cronExpression,opParams) {
			common.trace("Group("+_id+").scheduleOperationUsingCron(name="+name+", cronExpression="+cronExpression+
					", opParams={"+common.objToString(opParams)+"})");
			if(_getCategory() == GroupCategory.MIXED ){
				throw "It's not possible to invoke operation on MIXED group!!"
			}
			
			// get resources in this group
			var groupRes = _resources();
			var res = groupRes[0];
			// check that operation is correct and get operation configuration
			var conf = res.checkOperation(name,opParams);
			
			var opShedule = OperationManager.scheduleGroupOperationUsingCron(_id, null, true, name, common.hashAsConfiguration(conf), 
					cronExpression, 0, null)
			common.info("Group operation '"+name+"' scheduled on '"+_name+"'");
		},
		/**
		 * Runs operation on this group and returns result of the operation.
		 *
         * @param {Object} params - inputs for scheduling with fileds as follows:
		 *  <ul>
         *      <li>{String} name - name of operation (required)</li>
         *      <li>{boolean} haltOnFailure (optional, default is true)</li>
         *      <li>{Array} executionOrderResources defines execution order (optional)</li>
         *      <li>{String} description (optional)</li>
         *      <li>{Object} config - hashmap for operation params (Configuration) (optional)</li>
		 *  </ul>
         *
		 * @example allAgents.runOperation({name:"executeAvailabilityScan"});
		 *
		 */
		runOperation : function(params){
            params = params || {};
            params.haltOnFailure = params.haltOnFailure || true;
            params.executionOrderResources = params.executionOrderResources || [];
            params.description = params.description || null;
            params.config = params.config || null;
            var name,haltOnFailure,executionOrderResourceIds,description,opParams
			var groupOpShedule = _scheduleOperation(params.name,0,0,0,0,params.haltOnFailure,params.executionOrderResources.map(function(r){return r.id;}),
					params.description,params.config);
			var result = _waitForOperationResult(groupOpShedule);
			
			var ret = {}
			ret.status = String(result.status)
			ret.error = String(result.errorMessage)
			ret.nativeHistory = result;
			
			return ret;
		},
		/**
		 * Schedules operation on this group. In contrast to runOperation this is
		 * not blocking operation.
		 *
         *  @param {Object} params - inputs for scheduling with fileds as follows:
         *  <ul>
         *      <li>{String} name of operation (required)</li>
         *      <li>{Number} delay of operation in seconds (optional, 0 is default)</li>
         *      <li>{Number} repeatInterval of operation in seconds (optional, 0 is default)</li>
         *      <li>{Number} repeatCount of operations (optional, 0 is default)</li>
         *      <li>{Number} timeout of operation in seconds (optional, 0 is default)</li>
         *      <li>{boolean} haltOnFailure (optional, default is true)</li>
         *      <li>{Array} executionOrderResources defines execution order (optional)</li>
         *      <li>{String} description (optional)</li>
         *      <li>{Object} config - hashmap for operation params (Configuration) (optional)</li>
         *      <li></li>
         *      <li></li>
         *  </ul>
		 * @example scheduleOperation({name:"executeAvailabilityScan",delay:10,repeatInterval:10,repeatCount:10,config:{changesOnly:false});
		 *
		 */
		scheduleOperation : function(params){
            params = params || {};
            params.delay = params.delay || 0;
            params.repeatInerval = params.repeatInterval || 0;
            params.repeatCount = params.repeatCount || 0;
            params.timeout = params.timeout || 0;
            params.executionOrderResources = params.executionOrderResources || [];
            params.description = params.description || null;
            params.config = params.config || null;

			_scheduleOperation(params.name,params.delay,params.repeatInterval,params.repeatCount,params.timeout,
					params.haltOnFailure,params.executionOrderResources.map(function(r){return r.id;}),params.description,params.config);
		},
		/**
		 * Returns Array with metric intervals of given metric for all resources in this group.
		 * 
		 * @param {String}
		 *  		metricName 
		 * 
		 * @type {Array}
		 * @returns Array with javascript objects (hashmap) with following keys:
		 * <ul>
		 * <li>id {String} - resource id</li>
		 * <li>interval {Number} - metric collection interval</li>
		 * <ul>
		 */
		getMetricIntervals : function(metricName){
			// TODO - for all metric methods - create inner Metric type like its done for Resource, compatible group checks, tests
			common.debug("Getting metric intervals for metric with name " + metricName);
			var schedules = _getMetricSchedules(metricName);
			var array = new Array();
			for(var i =0;i<schedules.size();i++){
				array[i] = {id:new String(schedules.get(i).getResource().getId()),
						interval: new Number(schedules.get(i).getInterval())}
			}
			return array;
		},
		/**
		 * Returns Array with metric statuses of given metric for all resources in this group.
		 * 
		 * @param {String}
		 *  		metricName 
		 * 
		 * @type {Array}
		 * @returns Array with javascript objects (hashmap) with following keys:
		 * <ul>
		 * <li>id {String} - resource id</li>
		 * <li>isEnabled {Boolean} - true when metric is enabled</li>
		 * <ul>
		 */
		getMetricStatuses : function(metricName){
			common.debug("Getting metric statuses for metric with name " + metricName);
			var schedules = _getMetricSchedules(metricName);
			var array = new Array();
			for(var i =0;i<schedules.size();i++){
				array[i] = {id: new String(schedules.get(i).getResource().getId()),
						isEnabled: schedules.get(i).isEnabled()}
			}
			return array;
		},
		/**
		 * Returns true only if given metric is enabled on all resources in this group
		 * 
		 * @param {String}
		 *            metricName 
		 */
		isMetricEnabled : function(metricName){
			var schedules = _getMetricSchedules(metricName);
			for(var i =0;i<schedules.size();i++){
				if(!schedules.get(i).isEnabled()){
					return false;
				}
			}
			return true;
		},
		/**
		 * Returns true only if given metric is disabled on all resources in this group
		 * 
		 * @param {String}
		 *            metricName
		 */
		isMetricDisabled : function(metricName){
			var schedules = _getMetricSchedules(metricName);
			for(var i =0;i<schedules.size();i++){
				if(schedules.get(i).isEnabled()){
					return false;
				}
			}
			return true;
		}
		
	}
};

/**
 * creates a new instance of Resource Type
 * @class 
 * @constructor
 * @param {org.rhq.core.domain.resource.ResourceType} rhqType
 */
var ResourceType = function(rhqType) {	
    var common = new _common();
    if (!rhqType) {
		throw "org.rhq.core.domain.resource.ResourceType parameter is required";
	}
	var _obj = rhqType;
    /**
	 * @lends ResourceType.prototype
	 */
	return {
		/**
		 * resource type id
		 * @type Number
		 */
		id : _obj.id,
		/**
		 * resource type name
		 * @type String
		 */
		name: _obj.name,
		/**
		 * org.rhq.core.domain.resource.ResourceType instance
		 * @type org.rhq.core.domain.resource.ResourceType
		 */
		obj: _obj,
		/**
		 * plugin name defining this resource type
		 * @type String
		 */
		plugin: _obj.plugin,
		/**
		 * @function
		 * Returns default configuration for this resource type as hash.
		 * 
		 * @type hash
		 * @return default configuration for this resource type as hash
		 */
		getDefaultConfiguration : function(){
			common.trace("resourceType.getDefaultConfiguration()");
			var configDef = ConfigurationManager.getResourceConfigurationDefinitionForResourceType(_obj.id);
			var conf = org.rhq.core.domain.configuration.ConfigurationUtility.createDefaultConfiguration(configDef);
			
			return common.configurationAsHash(conf,configDef);
		}
	};
};

/**
 * 
 * @namespace provides access to resource types
 */
var resourceTypes = (function() {
	var common = new _common();
	return {
		/**
		 * @ignore
		 */
	    createCriteria : function(params) {
			params = params || {};
			common.trace("resourceTypes.createCriteria("+common.objToString(params) +")");
			var criteria = common.createCriteria(new ResourceTypeCriteria(),params,function(key,value) {
				if (key=="createDeletePolicy") {
		    		 return "addFilterCreateDeletePolicy(CreateDeletePolicy."+value.toUpperCase()+")";
		    	}
		    	if (key=="category") {
		    		return "addFilterCategories(ResourceCategory."+value.toUpperCase()+")";
		    	}
		    	if (key=="plugin") {
		    		return "addFilterPluginName(\""+value+"\")";
		    	}
			});
			// by default only 200 items are returned, this line discards it ..
			// so we get unlimited list
			criteria.clearPaging();
			criteria.fetchResourceConfigurationDefinition(true);
			criteria.fetchPluginConfigurationDefinition(true);
			
			return criteria;
		},
		/**
		 * @function
		 * Finds a resource type according to criteria params
		 * @param params - filter params
		 * * There are also shortcuts for ENUM parameters: - you can use
		 * <ul>
		 *    <li>{createDeletePolicy:"both"} insetead of {createDeletePolicy:"CreateDeletePolicy.BOTH"}</li>
		 *    <li>{category:"platform"} instead of {category:"ResourceCategory.PLATTFORM"}</li>
		 *    <li>{plugin:"Platforms"} instead of {pluginName:"Platforms"}</li>
		 *  </ul>
		 * @type ResourceType[]
		 * @return array of resource types
		 */
		find : function(params) {
			params = params || {};
			common.trace("resourceTypes.find("+common.objToString(params)+")");
			var criteria = resourceTypes.createCriteria(params);
			var res = ResourceTypeManager.findResourceTypesByCriteria(criteria);
			common.debug("Found "+res.size()+" resource types ");
		    return common.pageListToArray(res).map(function(x){return new ResourceType(x);});			
		}
	}
}) ();

/**
 * @namespace provides access to metric templates
 */
var metricsTemplates = (function() { 
  var common = new _common();
  
  var forEachMetricDef = function(resTypes, fn) {
	  resTypes.forEach(function(rt) {
		  var metricDefinitions = java.util.ArrayList(rt.obj.metricDefinitions);
		    for (i = 0; i < metricDefinitions.size(); ++i) {
		      var metricDef = metricDefinitions.get(i);
		      fn(rt,metricDef);
		    }
	  });    
  };

  var fetchMetricDefs = function(resTypes) {
	  resTypes = resTypes || []
	  var criteria = resourceTypes.createCriteria({ids:resTypes.map(function(rt){return rt.id})});
	  criteria.fetchMetricDefinitions(true);
	  var types = ResourceTypeManager.findResourceTypesByCriteria(criteria);
	  common.debug("Found "+types.size()+" resource types ");
	  return common.pageListToArray(types).map(function(x){return new ResourceType(x);});
  };

  return {
    /**
     * @namespace metric template predicates
     */
    predicates: {
      /**
       * predicate that accepts metric definitions of CALLTIME DataType
       * @field
       */
      isCallTime: function(metricDef) { 
        return metricDef.dataType == DataType.CALLTIME;
      },

      /**
       * predicate that accepts metric definitions of MEASUREMENT DataType
       * @field
       */
      isNumeric: function(metricDef) {
        return metricDef.dataType == DataType.MEASUREMENT;
      },
      /**
       * predicate that accepts metric definitions of TRAIT DataType
       * @field
       */
      isTrait: function(metricDef) {
          return metricDef.dataType == DataType.TRAIT;
      }
    },

    /**
     * disables metrics 
     * @public
     * @param {ResourceType[]} resTypes - resource types, see {@link resourceTypes.find}
     * @param filter - filter function - see {@link metricsTemplates.predicates}
     * @example metricsTemplates.disable(resourceTypes.find({name: "server-b", plugin: "PerfTest"}), metricTemplates.predicates.isCallTime);
     * @example metricsTemplates.disable(resourceTypes.find());
     */
    disable: function(resTypes, filter) {
    	common.trace("metricsTemplates.disable(resTypes="+resTypes+",filter="+filter+")");
    	resTypes = fetchMetricDefs(resTypes);
    	if (resTypes.length == 0) {
    		common.error("Failed to find resource types for " + resTypes);
        	return;
      	}
    	var definitionIds = [];
      	forEachMetricDef(resTypes, function(rt,metricDef) {
      		if (typeof filter == "undefined" || filter(metricDef)) {
      			common.debug("Preparing to disable metric template " + metricDef + " for " +rt.name);
      			definitionIds.push(metricDef.id);
        	}
      	});
      	MeasurementScheduleManager.disableSchedulesForResourceType(definitionIds, true);
    },
    
    /**
     * enables metrics 
     * @public
     * @param {ResourceType[]} resTypes - resource types, see {@link resourceTypes.find}
     * @param filter - filter function - see {@link metricsTemplates.predicates}
     * @example metricsTemplates.enable(resourceTypes.find({name: "server-b", plugin: "PerfTest"}), metricTemplates.predicates.isCallTime);
     * @example metricsTemplates.enable(resourceTypes.find());
     */
    enable: function(resTypes, filter) {
    	common.trace("metricsTemplates.enable(resTypes="+resTypes+",filter="+filter+")");
    	resTypes = fetchMetricDefs(resTypes);
    	if (resTypes.length == 0) {
    		common.error("Failed to find resource types for " + resTypes);
        	return;
      	}
    	var definitionIds = [];
      	forEachMetricDef(resTypes, function(rt,metricDef) {
      		if (typeof filter == "undefined" || filter(metricDef)) {
      			common.debug("Preparing to enable metric template " + metricDef + " for " +rt.name);
      			definitionIds.push(metricDef.id);
        	}
      	});
      	MeasurementScheduleManager.enableSchedulesForResourceType(definitionIds, true);
    },

    /**
     * sets collection interval for metrics 
     * @public
     * @param {ResourceType[]} resTypes - resource types, see {@link resourceTypes.find}
     * @param interval - collection interval in seconds
     * @param filter - filter function - see {@link metricsTemplates.predicates}
     * @example metricsTemplates.setCollectionInterval(resourceTypes.find({name: "server-b", plugin: "PerfTest"}), 30, metricTemplates.predicates.isCallTime);
     */
    setCollectionInterval: function(resTypes, interval, filter) {
    	common.trace("metricsTemplates.setCollectionInterval(resTypes="+resTypes+",interval="+interval+",filter="+filter+")");
    	resTypes = fetchMetricDefs(resTypes);
    	if (resTypes.lentgh == 0) {
    		common.error("Failed to find resource types for " + resTypes);
    		return;
    	}
    	var definitionIds = [];
      	forEachMetricDef(resTypes, function(rt,metricDef) {
      		if (typeof filter == "undefined" || filter(metricDef)) {
          	common.debug("Setting collection interval for metric template " + metricDef + " to " + interval + "s for resource type " + rt.name);
          	definitionIds.push(metricDef.id);
      		}
      	});
      	MeasurementScheduleManager.updateSchedulesForResourceType(definitionIds, interval * 1000, true);
    }
  };
})();


/**
 * @namespace Provides access to dynamic group definitions
 */
var dynaGroupDefinitions = (function(){
	var common = new _common();
	
	/** 
	 * Sets up given native GroupDefinition according to given parameters 
	 * @private
	 * @param {org.rhq.core.domain.resource.group.GroupDefinition} groupDefinition native groupDefinition to set up
	 * @param {Object} params
	 * @returns {org.rhq.core.domain.resource.group.GroupDefinition} prepared native groupDefinition
	 * @throws some of given parameters are not valid
	 */
	var _setUpGroupDefinition = function(groupDefinition,params){
		for (var k in params) {
		    // use hasOwnProperty to filter out keys from the
			// Object.prototype
		    if (params.hasOwnProperty(k)) {
		        var _k = k[0].toUpperCase()+k.substring(1);
		        var func = eval("groupDefinition.set"+_k);
		        if(typeof func == "undefined"){
		        	throw "Given parameter '"+_k+"' is not defined on org.rhq.core.domain.resource.group.GroupDefinition object";
		        }
		        func.call(groupDefinition,params[k]);
		    }
		}
		
		return groupDefinition;
	};
	var _find = function(params){
		params = params || {};
		common.debug("Searching for dynagroup definition with params: "+common.objToString(params));
		var cri = common.createCriteria(new ResourceGroupDefinitionCriteria(),params);
		cri.fetchManagedResourceGroups(true);
		cri.setStrict(true);
		var result = GroupDefinitionManager.findGroupDefinitionsByCriteria(cri);
	
		return common.pageListToArray(result).map(function(x){return new DynaGroupDefinition(x);});
	};
	
	return{
		/**
		 * Finds dynagroup definitions according to given parameters.
		 * @function
		 * @param {Object} params - see ResourceGroupDefinitionCriteria.addFilter[param] methods for available params.
		 * @example dynaGroupDefinitions.find({name:"All agents",description:"All agents in inventory"});
		 * @returns array of dynagroup definitions
		 * @type DynaGroupDefinition[]
		 */
		find : _find,
		/**
		 * Creates a new dynagroup definition with given parameters.
		 * @param {Object} params - see org.rhq.core.domain.resource.group.GroupDefinition.set[param] methods for available params.
		 * @example dynaGroupDefinitions.create({name:"All agents",description:"All agents in inventory",expression:"resource.type.name=RHQ Agent"});
		 * @type DynaGroupDefinition
		 * @return created dynagroup definition
		 */
		create : function(params){
			params = params || {};
			common.info("Creating dynagroup definition with params: "+common.objToString(params));
			var nativeGroupDef = _setUpGroupDefinition(new GroupDefinition(),params);
			nativeGroupDef = GroupDefinitionManager.createGroupDefinition(nativeGroupDef);
			
			return new DynaGroupDefinition(nativeGroupDef);
		},
		/**
		 * Edits existing dynagroup definition with given name using given parameters.
		 * @param {String} dynagroupDefName - name of dynagroup definition to be edited
		 * @param {Object} params - see org.rhq.core.domain.resource.group.GroupDefinition.set[param] methods for available params.
		 * @example dynaGroupDefinitions.edit("All agents",{name:"All agents - edited",recursive:true});
		 * @type DynaGroupDefinition
		 * @return updated dynagroup definition or null when dynagroup definition with given name was not found 
		 */
		edit : function(dynagroupDefName,params){
			params = params || {};
			common.info("Editing dynagroup definition with name: "+dynagroupDefName+", using params: "+common.objToString(params));
			var foundDynagroupDefs = _find({name:dynagroupDefName});
			if(foundDynagroupDefs.length >0){
				var nativeDynagroupDefOrig = foundDynagroupDefs[0].obj;
				var nativeDynagroupDefEdited = _setUpGroupDefinition(nativeDynagroupDefOrig,params);
				nativeDynagroupDefEdited = GroupDefinitionManager.updateGroupDefinition(nativeDynagroupDefEdited);
				
				return new DynaGroupDefinition(nativeDynagroupDefEdited);
			}else{
				common.warn("Dynagroup definition with name: "+dynagroupDefName+" was not found. Nothing to edit.");
				
				return null;
			}
		},
		/**
		 * Removes dynagroup definition with given name.
		 * @param {String} dynagroupDefName - name of dynagroup definition to be deleted
		 */
		remove : function(dynagroupDefName){
			common.info("Removing dynagroup definition with name: "+dynagroupDefName);
			var foundDynagroupDefs = _find({name:dynagroupDefName});
			if(foundDynagroupDefs.length >0){
				GroupDefinitionManager.removeGroupDefinition(foundDynagroupDefs[0].id);
			}else{
				common.warn("Dynagroup definition with name: "+dynagroupDefName+" was not found. Nothing to delete.");
			}
		}
	};
})();
/**
 * @class
 * @constructor
 */
var DynaGroupDefinition = function(param) {
	var common = new _common();
	common.trace("new DynaGroupDefinition("+param+")");
	if (!param) {
		throw "org.rhq.core.domain.resource.group.GroupDefinition parameter is required";
	}
	var _id = param.id;
	var _obj = param;
	
	/**
	 * @lends DynaGroupDefinition.prototype
	 */
	return {
		/**
		 * id of this dynagroup definition
		 * @field
		 * @type Number
		 */
		id : _id,
		/**
		 * native object
		 * @type org.rhq.core.domain.resource.group.GroupDefinition 
		 * @field
		 */
		obj : _obj,
		/**
		 * name of this dynagroup definition
		 */
		name : _obj.getName(),
		/**
		 * Returns groups managed by this dynagroup definition.
		 * @type ResGroup[]
		 * @return groups managed by this dynagroup definition
		 */
		managedGroups : function() {
			var groups =  _obj.getManagedResourceGroups();
			return groups.toArray().map(function(x){return new ResGroup(x);});
		},
		/**
		 * Removes this dynagroup definition.
		 */
		remove : function(){
			common.info("Removing dynaGroup definition with name: '" + _obj.getName() +"' and id: "+_id);
			GroupDefinitionManager.removeGroupDefinition(_id);
		},
		/**
		 * Explicitly recalculates groups managed by this dynagroup definition.
		 */
		recalculate : function(){
			common.info("Recalculating dynagroup definition with name: '"+_obj.getName()+"'");
			GroupDefinitionManager.calculateGroupMembership(_id);
		}
	}
}

/**
 * @namespace Provides access to drift subsystem
 */
var drifts = (function(){
	var common = new _common();
	
	return{
		/**
		 * Finds drift definition templates according to given parameters.
		 * @function
		 * @param {Object} params - see DriftDefinitionTemplateCriteria.addFilter[param] methods for available params.
		 * @example drifts.findDriftDefinitionTemplates({resourceTypeId:1});
		 * @returns array of drift definition templates
		 * @type DriftDefinitionTemplate[]
		 */
		findDriftDefinitionTemplates : function(params){
			params = params || {};
			common.trace("drifts.findDriftDefinitionTemplates("+common.objToString(params)+")");
			var cri = common.createCriteria(new DriftDefinitionTemplateCriteria(),params);
			cri.fetchDriftDefinitions(true);
			cri.fetchResourceType(true);
			var result = DriftTemplateManager.findTemplatesByCriteria(cri);
		
			return common.pageListToArray(result).map(function(x){return new DriftDefinitionTemplate(x);});
		},
		/**
		 * Finds drift definitions according to given parameters.
		 * @function
		 * @param {Object} params - see DriftDefinitionCriteria.addFilter[param] methods for available params.
		 * @example drifts.findDriftDefinition({name:"testDriftDefinition"});
		 * @returns array of drift definitions
		 * @type DriftDefinition[]
		 */
		findDriftDefinition : function(params){
			params = params || {};
			common.trace("drifts.findDriftDefinition("+common.objToString(params)+")");
			var cri = common.createCriteria(new DriftDefinitionCriteria(),params);
			var result = DriftManager.findDriftDefinitionsByCriteria(cri);
		
			return common.pageListToArray(result).map(function(x){return new DriftDefinition(x);});
		}
	};
})();
/**
 * @class
 * @constructor
 */
var DriftDefinitionTemplate = function(param) {
	var common = new _common();
	common.trace("new DriftDefinitionTemplate("+param+")");
	if (!param) {
		throw "org.rhq.core.domain.drift.DriftDefinitionTemplate parameter is required";
	}
	var _id = param.id;
	var _obj = param;
	
	/**
	 * @lends DriftDefinitionTemplate.prototype
	 */
	return {
		/**
		 * id of this drift definition template
		 * @field
		 * @type Number
		 */
		id : _id,
		/**
		 * native object
		 * @type org.rhq.core.domain.drift.DriftDefinitionTemplate
		 * @field
		 */
		obj : _obj,
		/**
		 * name of this drift definition template
		 */
		name : _obj.getName()
	}
}
/**
 * @class
 * @constructor
 */
var DriftDefinition = function(param) {
	var common = new _common();
	common.trace("new DriftDefinition("+param+")");
	if (!param) {
		throw "org.rhq.core.domain.drift.DriftDefinition parameter is required";
	}
	var _id = param.id;
	var _obj = param;
	
	/**
	 * @lends DriftDefinition.prototype
	 */
	return {
		/**
		 * id of this drift definition
		 * @field
		 * @type Number
		 */
		id : _id,
		/**
		 * native object
		 * @type org.rhq.core.domain.drift.DriftDefinition
		 * @field
		 */
		obj : _obj,
		/**
		 * name of this drift definition
		 */
		name : _obj.getName()
	}
}

/**
 * @namespace provides access to StorageNodes subsystem
 */
var storageNodes = (function() {
	var common = new _common();

	var _find = function(params) {
		params = params || {};
		common.trace("storageNodes.find(" + common.objToString(params)
				+ ")");
		var criteria = storageNodes.createCriteria(params);
		var result = StorageNodeManager.findStorageNodesByCriteria(criteria);
		common.debug("Found " + result.size() + " storageNodes ");
		return common.pageListToArray(result).map(function(x) {
			return new StorageNode(x);
		});
	};
	
	var _allAlerts = function(params){
		params = params || {};
		common.trace("storageNodes.allAlerts(" + common.objToString(params)
				+ ")");
		var result = StorageNodeManager.findAllStorageNodeAlerts();
		common.debug("Found " + result.size() + " storageNodes ");
		return common.pageListToArray(result).map(function(x) {
			return new Alert(x);
		});
	};
	/**
	@lends storageNodes
	*/
	return {

		/**
		 * creates StorageNodeCriteria object based on given params
		 * 
		 * @param {Object}
		 *            params - filter parameters
		 * @ignore
		 */
		createCriteria : function(params) {
			params = params || {};
			common.trace("storageNodes.createCriteria("
					+ common.objToString(params) + ")");
			var criteria = common.createCriteria(new StorageNodeCriteria(),
					params);
			return criteria;
		},

		/**
		 * finds storageNodes based on query parameters
		 * 
		 * @param {Object}
		 *            params - hash of query params See StorageNodeCriteria
		 *            class for available params
		 * @type StorageNode[]
		 * @function
		 */
		find : _find,
		
		/**
		 * gets all alerts for all storage nodes
		 * 
		 * @type Alert[]
		 * @function
		 */
		allAlerts : _allAlerts
		
	};
})();

/**
 * @class
 * @constructor
 */
var StorageNode = function(param) {
	var common = new _common();
	// we define StorageNode child classes as hidden types

	
	/**
	 *@lends StorageNode.prototype
	 */
	return {
		/**
		 * id of StorageNode
		 * @field
		 * @type String
		 *  
		 */
		id : param.id,
		/**
		 * StorageNode instance
		 * @field
		 * @type StorageNode
		 */
		obj : param,
		
		/**
		 * JMXConnectionURL of StorageNode
		 * @field
		 * @type String
		 */
		JMXConnectionURL : param.JMXConnectionURL,
		/**
		 * address of StorageNode
		 * @field
		 * @type String
		 */
		address : param.address,
		/**
		 * cqlPort of StorageNode
		 * @field
		 * @type String
		 */
		cqlPort : param.cqlPort,
		/**
		 * ctime of StorageNode
		 * @field
		 * @type String
		 */
		ctime : param.ctime,
		/**
		 * jmxPort of StorageNode
		 * @field
		 * @type String
		 */
		jmxPort : param.jmxPort,
		/**
		 * mtime of StorageNode
		 * @field
		 * @type String
		 */
		mtime : param.mtime,
		/**
		 * operationMode of StorageNode
		 * @field
		 * @type String
		 */
		operationMode : param.operationMode,
		/**
		 * resource of StorageNode
		 * @field
		 * @type Resource
		 */
		resource : param.resource

	};
};
/**
 * @class
 * @constructor
 */
var Alert = function(param){
	/**
	 *@lends Alert.prototype
	 */
	return {
		/**
		 * id of Alert
		 * @field
		 * @type String
		 */
		id : param.id,
		/**
		 * AlertDefinition instance
		 * @field
		 * @type AlertDefinition
		 */
		alertDefinition: param.alertDefinition,
		/**
		 * alert notification logs
		 * @field
		 * @type String[]
		 */
		alertNotificationLogs: param.alertNotificationLogs
	}
	
};


/**
 * @namespace provides access to AlertDefinition subsystem
 */
var alertDefinitions = (function() {
	var common = new _common();

	var _find = function(params) {
		params = params || {};
		common.trace("alertDefinitions.find(" + common.objToString(params)
				+ ")");
		var criteria = alertDefinitions.createCriteria(params);
		var result = AlertDefinitionManager
				.findAlertDefinitionsByCriteria(criteria);
		common.debug("Found " + result.size() + " alertDefinitions ");
		return common.pageListToArray(result).map(function(x) {
			return new AlertDefinition(x);
		});
	};
	/**
	@lends alertDefinitions
	*/
	return {

		/**
		 * creates AlertDefinitionCriteria object based on given params
		 * 
		 * @param {Object}
		 *            params - filter parameters
		 * @ignore
		 */
		createCriteria : function(params) {
			params = params || {};
			common.trace("alertDefinitions.createCriteria("
					+ common.objToString(params) + ")");
			var criteria = common.createCriteria(new AlertDefinitionCriteria(),
					params);
			return criteria;
		},

		/**
		 * finds alertDefinitions based on query parameters
		 * 
		 * @param {Object}
		 *            params - hash of query params See AlertDefinitionCriteria
		 *            class for available params
		 * @type AlertDefinition[]
		 * @function
		 */
		find : _find

	};
})();

/**
 * @class
 * @constructor
 */
var AlertDefinition = function(param) {
	var common = new _common();
	// we define AlertDefinition child classes as hidden types

	var _id = param.id;
	var _obj = param;

	/**
	 * @name AlertDefinition-Condition
	 * @class
	 * @constructor
	 */
	var Condition = function(param) {
		common.trace("new Condition(" + param + ")");
		if (!param) {
			throw "either Number or org.rhq.core.domain.alert.AlertDefinition parameter is required";
		}
		var _id = param.id;
		var _obj = param;

		/**
		 * @lends AlertDefinition-Condition.prototype
		 */
		return {
			/**
			 * Condition id
			 * 
			 * @field
			 */
			id : param.id,
			/**
			 * Condition instance
			 * 
			 * @field
			 * @type Condition
			 */
			obj : _obj,
			/**
			 * threshold of condition
			 * @field
			 * @type String
			 */
			threshold : param.threshold,
			/**
			 * name of condition
			 * 
			 * @field
			 * @type String
			 */
			name : param.name,
			/**
			 * comparator of condition
			 * @field
			 * @type String
			 */
			comparator : param.comparator,
			/**
			 * alertDefinition of condition
			 * @field
			 * @type AlertDefinition
			 */

			alertDefinition : param.alertDefinition,
			/**
			 * triggerId of condition
			 * @field
			 * @type String
			 */
			triggerId : param.triggerId

		};

	};
	/**
	 *@lends AlertDefinition.prototype
	 */
	return {
		/**
		 * id of AlertDefinition
		 * @field
		 * @type String
		 *  
		 */
		id : _id,
		/**
		 * AlertDefinition instance
		 * @field
		 * @type AlertDefinition
		 */
		obj : _obj,
		/**
		 * name of AlertDefinition
		 * @field
		 * @type String
		 *  
		 */
		name : param.name,
		/**
		 * gets conditions of AlertDefinition
		 */
		conditions : function() {
			common.trace("AlertDefinition(" + _id + ").conditions()");
			var criteria = alertDefinitions.createCriteria({
				id : _id
			});
			criteria.fetchConditions(true);
			var result = AlertDefinitionManager
					.findAlertDefinitionsByCriteria(criteria);
			if (result.size() == 1 && result.get(0).conditions) {
				result = result.get(0).conditions.toArray();
				return result.map(function(x) {
					return new Condition(x);
				});
			};
			
		}

	};
};


/**
 * @namespace provides access to Bundle groups
 */
var bundleGroups = (function() {
    var common = new _common();

    return {
        /**
         * creates a org.rhq.domain.criteria.BundleGroupCriteria object based on
         * given params
         * 
         * @param {Obejct}
         *            params - criteria params
         * @returns BundleGroupCriteria
         * @ignore
         */
        createCriteria : function(params) {
            params = params || {};
            common.debug("bundleGroups.createCriteria(" + common.objToString(params) + ")");
            var criteria = common.createCriteria(new BundleGroupCriteria(), params);
            return criteria;
        },
        /**
         * finds bundle groups by given params
         * 
         * @param {Object}
         *            params see BundleGroupCriteria for available params
         * @type BundleGroup[]
         */
        find : function(params) {
            params = params || {};
            common.trace("bundleGroups.find(" + common.objToString(params) + ")");
            var criteria = bundleGroups.createCriteria(params);
            var result = BundleManager.findBundleGroupsByCriteria(criteria);
            common.debug("Found " + result.size() + " groups ");
            return common.pageListToArray(result).map(function(x) {
                return new BundleGroup(x);
            });
        },
        /**
         * creates a new bundle group. You can pass array of Bundles to become
         * members of new group
         * 
         * @param {String}
         *            name for a new group
         * @param {Bundle[]}
         *            children - array of resources that represents content of
         *            this group
         * @type BundleGroup
         * @return {BundleGroup}
         */
        create : function(name, children) {
            children = children || [];
            common.info("Creating a group '" + name + "', with following children: '" + common.objToString(children) + "'");
            var group = BundleManager.createBundleGroup(new org.rhq.core.domain.bundle.BundleGroup(name));
            BundleManager.assignBundlesToBundleGroups([ group.id ], children.map(function(x) {
                return x.getId();
            }));
            return new BundleGroup(group);
        }
    };
})();


/**
 * @class
 * @constructor
 */
var BundleGroup = function(param) {
    var common = new _common();
    common.trace("new BundleGroup(" + param + ")");
    if (!param) {
        throw "either number or org.rhq.core.domain.bundle.BundleGroup parameter is required";
    }
    var _id = param.id;
    var _obj = param;
    var _name = param.name;

    /**
     * @lends BundleGroup.prototype
     */
    return {
        /**
         * gets ID of this group
         * 
         * @field
         */
        id : _id,
        /**
         * gets name of this group
         * 
         * @field
         */
        name : _name,
        /**
         * gets underlying ResourceGroup instance
         * 
         * @field
         * 
         */
        obj : _obj,
        /**
         * returns ID of this group
         * 
         * @function
         * @type Number
         */
        getId : function() {
            return _id;
        },
        /**
         * assigns bundles to this group, does nothing if bundles are already in this group
         * @param {Bundle[]} bundles to assign
         */
        assignBundles : function(bundleArray) {
            common.trace("BundleGroup("+_id+").assignBundles("+common.objToString(bundleArray)+")");
            bundleArray = bundleArray || [];
            BundleManager.assignBundlesToBundleGroups([_id],bundleArray.map(function(b){return b.id;}));
        },
        /**
         * unassigns bundles to this group, does nothing if bundles are not assigned with this group
         * @param {Bundle[]} bundles to assign
         */
        unassignBundles : function(bundleArray) {
            common.trace("BundleGroup("+_id+").unassignBundles("+common.objToString(bundleArray)+")");
            bundleArray = bundleArray || [];
            BundleManager.unassignBundlesFromBundleGroups([_id],bundleArray.map(function(b){return b.id;}));
        },
        /**
         * removes this bundle group
         */
        remove : function() {
            common.info("Removing a group with name '" + _name + "'");
            BundleManager.deleteBundleGroups([ _id ]);
        },
        /**
         * returns bundles assigned to this group
         * 
         * @type Bundle[]
         * 
         */
        bundles : function() {
            common.trace("BundleGroup("+_id+").bundles()");
            return bundles.find({bundleGroupIds:[_id]});
        }
    }
};

/**
 * @namespace provides access to Bundle subsystem
 */
var bundles = (function() {

  var common = new _common();

  var _find = function(params) {
		params = params || {};
		common.trace("bundles.find("+common.objToString(params)+")");
		var criteria = bundles.createCriteria(params);
		var result = BundleManager.findBundlesByCriteria(criteria);
		common.debug("Found "+result.size()+" budles ");
	  return common.pageListToArray(result).map(function(x){return new Bundle(x);});
	};

    _createFromDistFile = function(dist,username,password,groups) {
        if (dist==null) {
            throw "parameter dist must not be null"
        }
        groups = groups || null;
        if (groups!=null) {
            groups = groups.map(function(g){return g.id;})
        }
        var groupsSupported = typeof BundleManager.createInitialBundleVersionViaURL !== "undefined" && groups != null;
        if (groups!=null && groups.length>0 && !groupsSupported) {
            common.error('Bundle groups are not supported on this version of RHQ, groups parameter is ignored');
        }
        if (dist.indexOf("http")==0) {
            common.debug("Getting bundle file from URL: "+dist);
            username = username || null;
            password =  password || null;
            if (username!=null && password!=null) {
                if (groupsSupported) {
                    var version = BundleManager.createInitialBundleVersionViaURL(groups,dist,username,password);
                }
                else {
                    var version = BundleManager.createBundleVersionViaURL(dist,username,password);
                }
                return new Bundle(version.bundle);
            }
            if (groupsSupported) {
                var version = BundleManager.createBundleVersionViaURL(groups,dist);
            }
            else {
                var version = BundleManager.createBundleVersionViaURL(dist);
            }
            return new Bundle(version.bundle);
        }
        else {
            var file = new java.io.File(dist);
            if (!file.exists()) {
                throw "file parameter ["+file+"] does not exist!";
            }
            if (typeof scriptUtil.uploadContent !== "undefined") {
                // since JON 3.2 we can stream our content to server
                var handle = scriptUtil.uploadContent(file);
                if (groupsSupported) {
                    var version = BundleManager.createInitialBundleVersionViaContentHandle(groups,handle);
                }
                else {
                    var version = BundleManager.createBundleVersionViaContentHandle(handle);
                }
                return new Bundle(version.bundle);
            }
            else {
                // keep this to stay compatible with < JON 3.2
                common.debug("Getting bundle file from disk: '"+dist+"'");
                var inputStream = new java.io.FileInputStream(file);
                var fileLength = file.length();
                var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
                for (var numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
                    numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset);
                }
                if (groupsSupported) {
                    var version = BundleManager.createInitialBundleVersionViaByteArray(groups,fileBytes);
                }
                else {
                    var version = BundleManager.createBundleVersionViaByteArray(fileBytes);
                }
                return new Bundle(version.bundle);
            }
        }
    };

    return {
        /**
         * creates BundleCriteria object based on given params
         *
         * @param {Object} params - filter parameters
         * @ignore
         */
        createCriteria: function (params) {
            params = params || {};
            common.trace("bundles.createCriteria(" + common.objToString(params) + ")");
            var criteria = common.createCriteria(new BundleCriteria(), params);
            return criteria;
        },
        /**
         * finds bundles based on query parameters
         *
         * @param {Object} params - hash of query params
         * See BundleCriteria class for available params
         * @type Bundle[]
         * @function
         */
        find: _find,
        /**
         * creates a bundle (a wrapper method above createFromDistFile)
         * @see bundles#createFromDistFile
         * @param {Object} params
         * If URL it must be reachable by RHQ server
         * @type Bundle
         */
        create: function (params) {
            params = params || {}
            params.username = params.username || null;
            params.password = params.password || null;
            params.groups = params.groups || null;
            common.trace("bundles.create('params=" + common.objToString(params) + "')");
            return _createFromDistFile(params.dist, params.username, params.password, params.groups);
        },
        /**
         * creates a bundle (deprecated)
         *
         * @param {String} dist - path to bundle distribution ZIP file or URL.
         * @param {String} username - basic HTTP auth username (use when 'dist' is URL)
         * @param {String} password - basic HTTP auth password (use when 'dist' is URL)
         * @param {BundleGroup[]} groups - array of bundle groups to assign into
         * If URL it must be reachable by RHQ server
         * @type Bundle
         */
        createFromDistFile: function (dist, username, password, groups) {
            common.trace("bundles.createFromDistFile('" + dist + "','" + username + "','" + password + "','" + groups + "')");
            common.warn("You are using deprecated method, please use bundles.create instead");
            return _createFromDistFile(dist, username, password, groups);
        }
    };
})();


/**
 * @class
 * @constructor
 */
var Bundle = function(param) {

	// we define Bundle child classes as hidden types
	  /**
	   *  @name Bundle-Destination
		 * @class
		 * @constructor
		 */
	var Destination = function(param) {
		common.trace("new Destination("+param+")");
		if (!param) {
			throw "either number or org.rhq.core.domain.bundle.BundleDestination parameter is required";
		}
		var _id = param.id;
		var _obj = param;
		/**
		 * @lends Bundle-Destination.prototype
		 */
		return {
			/**
			 * destination id
			 */
			id : param.id,
			/**
			 * org.rhq.core.domain.bundle.BundleDestination instance
			 */
			obj : _obj,
			/**
			 * purges this destination
			 */
      purge : function() {
				common.trace("Destination("+_id+").purge()");
				BundleManager.purgeBundleDestination(_id);
			},
      /**
		 * reverts bundle deployment in this destination
		 *
		 * @param {boolean}
		 *            isClean
		 */
			revert : function(isClean) {
				if (isClean==null) {
					isClean = false;
				}
				common.trace("Destination("+_id+").revert(isClean[default=false]="+isClean+")");
				var deployment = BundleManager.scheduleRevertBundleDeployment(_id,null,isClean);
				var func = function() {
					var crit = common.createCriteria(new BundleDeploymentCriteria(),{id:deployment.id});
			        var result = BundleManager.findBundleDeploymentsByCriteria(crit);
			        if (!result.isEmpty()) {
			        	result = result.get(0);
			        	if (!(result.status == BundleDeploymentStatus.PENDING || result.status == BundleDeploymentStatus.IN_PROGRESS)) {
			        		return result;
			        	}
			        }
				};
				var deployment = common.waitFor(func);
				if (deployment) {
					common.info("Bundle deployment finished with status : "+deployment.status);
					return new Deployment(deployment);
				}
				throw "Bundle deployment error";
			}
		};
	};

	/**
	 * @name Bundle-Deployment
	 * @class
	 * @constructor
	 */
	var Deployment = function(param) {
		common.trace("new Deployment("+param+")");
		if (!param) {
			throw "either number or org.rhq.core.domain.bundle.BundleDeployment parameter is required";
		}
		var _id = param.id;
		var _obj = param;
		/**
		 * @lends Bundle-Deployment.prototype
		 */
		return {
			/**
			 * gets underlying BundleDeployment object
			 * @field
			 */
			obj : _obj,
			/**
			 * purges this deployment
			 */
			purge : function() {
				common.trace("Deployment("+_id+").purge()");
				if (_obj.isLive()) {
					BundleManager.purgeBundleDestination(_obj.destination.id);
				}
				else {
					throw "This Deployment("+_id+") cannot be purged, it is not LIVE";
				}

			}
		};
	};


	/**
	 * @name Bundle-Version
	 * @class
	 * @constructor
	 */
	var Version = function(param) {
		common.trace("new Version("+param+")");
		if (!param) {
			throw "either number or org.rhq.core.domain.bundle.BundleVersion parameter is required";
		}
		var _id = param.id;
		var _obj = param;
		/**
		 * @lends Bundle-Version.prototype
		 */
		return {
			/**
			 * gets this ID
			 * @field
			 */
			id : _id,
			/**
			 * gets underlying BundleVersion object
			 * @field
			 */
			obj : _obj,
			/**
			 * removes this version of bundle from server
			 */
			remove : function() {
				BundleManager.deleteBundleVersion(_id,false);
			},
			/**
			 * returns all files contained in this version of bundle (not yet implemented)
			 * @return array of filenames contained
			 * @type String[]
			 */
			files : function() {
				return BundleManager.getBundleVersionFilenames(_id,false).toArray().map(function (x) {return String(x);});
			}
		};
	};

	var common = new _common();
	common.trace("new Bundle("+param+")");
	if (!param) {
		throw "either number or rhq.domain.Bundle parameter is required";
	}
	var _id = param.id;
	var _bundle = param;
	var _destinations = function(params) {
		params = params || {};
		common.trace("Bundle("+_id+").destinations("+common.objToString(params)+")");
		params["bundleId"] = _id;
		var criteria = common.createCriteria(new BundleDestinationCriteria(),params, function(key,value) {
			if (key=="status"){
				return "addFilterStatus(BundleDeploymentStatus."+value.toUpperCase()+")";
			}
		});
		var result = BundleManager.findBundleDestinationsByCriteria(criteria);
		common.debug("Found "+result.size()+" destinations");
		return common.pageListToArray(result).map(function(x){return new Destination(x);});
	};
	var _versions = function(params) {
		params = params || {};
		common.trace("Bundle("+_id+").versions("+common.objToString(params)+")");
		params["bundleId"] = _id;
		var criteria = common.createCriteria(new BundleVersionCriteria(),params);
		var result = BundleManager.findBundleVersionsByCriteria(criteria);
		common.debug("Found "+result.size()+" versions");
		return common.pageListToArray(result).map(function(x){return new Version(x);});
	};

/**
 * @lends Bundle.prototype
 */
	return {
	    /**
	     * @return id of this bundle
	     * @type Number
	     */
	    id : _id,
	    /**
         * @return id of this bundle
         * @type Number
         */
	    getId : function() {return _id; },
		toString : function() {return _bundle.toString();},
		/**
		 * returns Bundle destinations based on query params
		 *
		 * @function
		 * @param {Object}params - filter
		 * See BundleResourceDeploymentCriteria class for available params
		 * There are also shortcuts for ENUM parameters: - you can use
		 * <ul>
		 *    <li>{status:"success"} insetead of {BundleDeploymentStatus:"BundleDeploymentStatus.SUCCESS"}</li>
		 *  </ul>
		 * @type Bundle-Destination[]
		 */
    destinations : _destinations,
		/**
		 * returns Bundle versions based on query params
		 *
		 * @function
		 * @param {Object} params - filter
		 * See BundleVersionCriteria class for available params
		 * @type Bundle-Version[]
		 */
		versions : _versions,
    /**
	 * deploys this bundle
	 *
	 * @param {Bundle-Destination} destination - destination to be deployed to
	 * @param {Object} params - map of input parameters required by bundle
	 * @param {Bundle-Version|String} version - bundle version to be deployed, if null, latest version is used
	 * @param {Boolean} isClean - whether to perform clean deployment or not, default is true
	 * @type Bundle-Deployment
	 */
		deploy : function(destination,params,version,isClean) {
			params = params || {};
			if (typeof(isClean) == "undefined") {
			    isClean = true;
			}
			var versionStr; // detect version argument and properly print it
			if (typeof(version) == "string") {
				versionStr = version;
			}
			else if (version != null && typeof(version) == "object") {
				versionStr = version.obj;
			}
			common.trace("Bundle("+_id+").deploy(destination="+common.objToString(destination)+",params="+common.objToString(params)+",version="+versionStr+")");
			if (version==null) {
				common.info("Param version is null, will use latest version of bundle");
				var criteria = common.createCriteria(new BundleCriteria(),{id:_id});
				var ver = BundleManager.findBundlesWithLatestVersionCompositesByCriteria(criteria).get(0).latestVersion;
				version = _versions({bundleId:_id,version:ver})[0];
			}
			if (typeof(version) == "string") {
				var versions = _versions({bundleId:_id,version:version});
				if (version.length==0) {
					throw "Param version ["+version+"] is invalid for this bundle - no such version";
				}
				version = versions[0];
			}
			// we need to fetch version object with configuration definition
			var criteria = common.createCriteria(new BundleVersionCriteria(),{id:version.id});
			criteria.fetchConfigurationDefinition(true);
			version.obj = BundleManager.findBundleVersionsByCriteria(criteria).get(0);
			var configuration = new Configuration();
			// so if the bundle has come configuration, we create default
			// instance of it and apply our param values
			if (version.obj.configurationDefinition.defaultTemplate!=null) {
				var defaultConfig = version.obj.configurationDefinition.defaultTemplate.createConfiguration();
				configuration = common.applyConfiguration(defaultConfig,version.obj.configurationDefinition,params);
			}
			var deployment = BundleManager.createBundleDeployment(version.obj.id, destination.id, "", configuration);
			deployment = BundleManager.scheduleBundleDeployment(deployment.id, isClean);
			var func = function() {
				var crit = common.createCriteria(new BundleDeploymentCriteria(),{id:deployment.id});
				crit.fetchResourceDeployments(true);
		        var result = BundleManager.findBundleDeploymentsByCriteria(crit);
		        if (!result.isEmpty()) {
		        	result = result.get(0);
		        	if (!(result.status == BundleDeploymentStatus.PENDING || result.status == BundleDeploymentStatus.IN_PROGRESS)) {
		        		return result;
		        	}
		        }
			};
			var deployment = common.waitFor(func);
			if (deployment) {
				common.info("Bundle deployment finished with status : "+deployment.status);
				if(deployment.status != BundleDeploymentStatus.SUCCESS){
				    common.error("Bundle deployment err msg: " +deployment.getErrorMessage());
				    var resDeployments = deployment.getResourceDeployments();
				    for(i = 0;i < resDeployments.size();i++){
				        var status = resDeployments.get(i).getStatus();
				        var name = resDeployments.get(i).getResource().getName();
				        common.info("Resource name: " +name+", status: "+status);
				        if(status != BundleDeploymentStatus.SUCCESS){
				            common.error("Bundle deployment on resource named " +name+" failed!");
				            var resCri = new BundleResourceDeploymentCriteria();
				            resCri.addFilterId(resDeployments.get(i).getId());
				            resCri.fetchHistories(true);
				            var resDep = BundleManager.findBundleResourceDeploymentsByCriteria(resCri).get(0);
				            
				            var hists = resDep.getBundleResourceDeploymentHistories();
				            for(x = 0; x < hists.size();x++){
				                if(hists.get(x).getStatus() != BundleResourceDeploymentHistory.Status.SUCCESS){
				                    common.error("Failed operation: "+hists.get(x).getInfo());
				                    common.error("Msg: "+hists.get(x).getMessage());
				                }
				            }
				        }
				    }
				}
				return new Deployment(deployment);
			}
			throw "Bundle deployment error";
		},
    /**
	 * creates a Bundle destination
	 *
	 * @param {ResGroup} group - must be COMPATIBLE and must contain resources supporting Bundle deployment
	 * @param {String} name of new destination, if null, name is taken from group
	 * @param {String} target - directory or path relative to `baseName` - if null,
	 * default is taken (this default defines ResourceType - so it will be based on baseName)
	 * @param {String} baseName - name of property found in group's ResourceType will
	 * be used as base when constructing target deploy directory - can be null when
	 * group's ResourceType defines exactly 1 baseName
	 * @type Bundle-Destination
	 */
		createDestination : function(group,name,target,baseName) {
			common.trace("Bundle("+_id+").createDestination(group="+group+",name="+name+",target="+target+",baseName="+baseName+")");
			if (group==null) {
				throw "Resource group param cannot be null";
			}
			// check whether we can deploy bundles to this group
			var g = groups.find({id:group.getId(),bundleTargetableOnly:true});
			if (g.length==0) {
				throw "Given resource group must contains only resources able to deploy bundles";
			}
			if (name==null) {
				name = group.obj.name;
				common.debug("Destination name is null, using group name ["+name+"] as name");
			}
			// wheck for valid baseName
			var criteria = common.createCriteria(new ResourceTypeCriteria(),{id:group.obj.resourceType.id});
			criteria.fetchBundleConfiguration(true);
			var resType = ResourceTypeManager.findResourceTypesByCriteria(criteria).get(0);
			var baseNames = {};
			var names = "";
			var iterator = resType.resourceTypeBundleConfiguration.bundleDestinationBaseDirectories.iterator();
			while(iterator.hasNext()) {
				var dest = iterator.next();
				baseNames[dest.name] = dest.valueName;
				names+=dest.name+", ";
			}
			var size = resType.resourceTypeBundleConfiguration.bundleDestinationBaseDirectories.size();
			if (size>1) {
				if (baseName==null) {
					throw "baseName parameter must NOT be null, because resource type for given group defines more than 1 baseNames ["+names+"]";
				}

			}
			if (baseName==null) {
				baseName = resType.resourceTypeBundleConfiguration.bundleDestinationBaseDirectories.iterator().next().name;
				common.info("Parameter baseName was not passed, using default : " +baseName)
			}
			if (typeof(baseNames[baseName]) == "undefined") {
				throw "Invalid baseName parameter, valid for given resource group are ["+names+"]";
			}
			if (target==null) {
				target = baseNames[baseName];
				common.info("Parameter target was not passed, using default : "+target);
			}
			// check for destinations having same target on same group
			_destinations({groupId:group.getId()}).forEach(function(d) {
				if (d.obj.destinationBaseDirectoryName==baseName && d.obj.deployDir == target) {
					throw "Destination under given group with given baseName and target already exists";
				}
			});
			var bundleDestination = BundleManager.createBundleDestination(_id,name, "", baseName, target, group.getId());
			return Destination(bundleDestination);
		},
		/**
		 * removes this bundle from server
		 */
		remove : function() {
			common.trace("Bundle("+_id+").remove()");
			BundleManager.deleteBundle(_id);
		}
	};

};

/**
 * @namespace provides access to resources in inventory
 */
var resources = (function () {
	var common = new _common();
	return {
		/**
		 * creates ResourceCriteria object based on filter
		 *
		 * @param {Object} params - query params
		 * @ignore
		 */
    createCriteria : function(params) {
			params = params || {};
			common.trace("resources.createCriteria("+common.objToString(params) +")");
			var criteria = common.createCriteria(new ResourceCriteria(),params,function(key,value) {
				if (key=="status") {
		    		 return "addFilterInventoryStatus(InventoryStatus."+value.toUpperCase()+")";
		    	}
		    	if (key=="category") {
		    		return "addFilterResourceCategories(ResourceCategory."+value.toUpperCase()+")";
		    	}
		    	if (key=="availability") {
		    		return "addFilterCurrentAvailability(AvailabilityType."+value.toUpperCase()+")";
		    	}
		    	if (key=="type") {
		    		return "addFilterResourceTypeName(\""+value+"\")";
		    	}
		    	if (key=="key") {
		    		return "addFilterResourceKey(\""+value+"\")";
		    	}
			});
			// by default only 200 items are returned, this line discards it ..
			// so we get unlimited list
			criteria.clearPaging();
			return criteria;
		},
		/**
		 * finds resources in inventory
		 *
		 * @param {Object} params - see ResourceCriteria.addFilter[param] methods for available params.
		 * There are also shortcuts for ENUM parameters: - you can use
		 * <ul>
		 *    <li>{status:"new"} insetead of {InventoryStatus:"InventoryStatus.NEW"}</li>
		 *    <li>{category:"platform"} instead of {ResourceCategory:"ResourceCategory.PLATTFORM"}</li>
		 *    <li>{availability:"up"} instead of {AvailabilityType:"AvailabilityType.UP}</li>
		 *    <li>{type:"RHQ Agent"} instead of {resourceTypeName:"RHQ Agent"}</li>
		 *  </ul>
		 * @example resources.find({type:"RHQ Agent",name:"RHQ Agent",availability:"down"});
		 * @example resources.find({type:"JBoss AS7 Standalone Server",parentResourceId:12345});
		 * @returns array of resources
		 * @type Resource[]
		 */
		find : function(params) {
			params = params || {};
			common.trace("resources.find("+common.objToString(params)+")");
			params.status="COMMITTED";
			var criteria = resources.createCriteria(params);
			var res = ResourceManager.findResourcesByCriteria(criteria);
			common.debug("Found "+res.size()+" resources ");
		    return common.pageListToArray(res).map(function(x){return new Resource(x);});
		},
		/**
		 *
		 * @returns array of platforms in inventory
		 * @type Array
		 */
		platforms : function(params) {
			params = params || {};
			common.trace("resources.platforms("+common.objToString(params) +"))");
			params['category'] = "PLATFORM";
			return resources.find(params);
		},
		/**
		 * returns 1st platform found based on given params or just nothing
		 *
		 * @type Resource
		 */
		platform : function(params) {
			params = params || {};
			common.trace("resources.platform("+common.objToString(params) +"))");
			params['category'] = "PLATFORM";
			var result = resources.find(params);
			if (result.length>0) {
				return result[0];
			}
		}
	};
}) ();

/**
 * @namespace provides access to discovery queue
 */
discoveryQueue = (function () {

	var common = new _common();

	var _waitForResources = function(criteria) {
		var res = common.waitFor(function() {
			var res = ResourceManager.findResourcesByCriteria(criteria);
			if (res.size()>0) {
				return res;
			}
		});
		if (res==null) {
			return new java.util.ArrayList();
		}
		return res;
	};
	var _importResources = function (params){
		params = params || {};
		common.trace("discoveryQueue.importResources("+common.objToString(params)+")");
		params.status="NEW";
		var criteria = resources.createCriteria(params);
	    common.info("Waiting until desired resources become NEW");
	    var res = _waitForResources(criteria);
	    common.debug("Found "+res.size()+" NEW resources");
	    var resourcesArray = common.pageListToArray(res);
	    // assertTrue(res.size()>0, "At least one resrouce was found");
	    DiscoveryBoss.importResources(resourcesArray.map(function(x){return x.id;}));
	    params.status="COMMITTED";
	    criteria = resources.createCriteria(params);
	    common.info("Waiting until resources become COMMITTED");
	    var committed = _waitForResources(criteria);
	    //assertTrue(committed.size() > 0, "COMMITED resources size > 0");
	    // return only imported resources
	    return common.pageListToArray(res).map(function(x){return new Resource(x);});
	};
  var _listPlatforms = function(params) {
			params = params || {};
			common.trace("discoveryQueue.listPlatforms("+common.objToString(params)+")");
			params["status"] = "NEW";
			params["category"] = "PLATFORM";
			var criteria = resources.createCriteria(params);
			var res = ResourceManager.findResourcesByCriteria(criteria);
			return common.pageListToArray(res).map(function(x){return new Resource(x);});
	};
	return {
		/**
		 * lists discovery queue
		 *
		 * @param {Object} params - filter
		 * @returns Array of resources in discovery queue matching given filter
		 * @type Resource[]
		 */
    find : function (params) {
			params = params || {};
			common.trace("discoveryQueue.list("+common.objToString(params)+")");
			params["status"] = "NEW";
			var criteria = resources.createCriteria(params);
			var res = ResourceManager.findResourcesByCriteria(criteria);
			return common.pageListToArray(res).map(function(x){return new Resource(x);});
		},
		/**
		 * lists discovery queue
		 * @deprecated use find() instead
		 * @param {Object} params - filter
		 * @returns Array of resources in discovery queue matching given filter
		 * @type Resource[]
		 */
    list : function (params) {
			params = params || {};
			common.trace("discoveryQueue.list("+common.objToString(params)+")");
			params["status"] = "NEW";
			var criteria = resources.createCriteria(params);
			var res = ResourceManager.findResourcesByCriteria(criteria);
			return common.pageListToArray(res).map(function(x){return new Resource(x);});
		},
	
		/**
		 * lists platforms from discovery queue
		 *
		 * @param {Object} params - filter
		 * @returns Array of platforms in discovery queue matching given filter
		 * @type Resource[]
		 * @function
		 */
    listPlatforms : _listPlatforms,
    /**
	 * imports platform by name
	 *
	 * @param {String} name - platform name to be imported
	 * @param {Booolean} children - if true (default) import also all child resources 
   * @returns platform resource
	 * @type Resource
	 */
		importPlatform : function(name, children) {
			common.trace("discoveryQueue.importPlatform(name=" + name + " children[default=true]=" + children + ")");

			// default is true (when null is passed)
			if (children != false) {
				children = true;
			}

			// first lookup whether platform is already imported
			var res = resources.find({name : name,category : "PLATFORM"});
			if (res.length == 1) {
				common.debug("Platform " + name + " is already in inventory, not importing");
				return res[0];
			}
			if (_listPlatforms({name : name}).length < 1) {
				throw "Platform [" + name + "] was not found in discovery queue"
			}
			res = _importResources({name : name,category : "PLATFORM"});
			if (res.length != 1) {
				throw "Plaform was not imported, server error?"
			}
			if (children) {
				common.debug("Importing platform's children");
				_importResources({parentResourceId : res[0].getId()});
			}
			common.debug("Waiting 15 seconds, 'till inventory syncrhonizes with agent");
			sleep(15 * 1000);
			return res[0];
		},
    /**
	 * imports resource
	 *
	 * @param {Resource} resource or ID - to be imported
	 * @param {Booolean}
	 *            children - if true (default) import also all child resources
	 *            (if any)
	 * @type Resource
	 */
		importResource : function(resource,children) {
			common.trace("discoveryQueue.importResource(resource="+resource+" children[default=true]="+children+")");
			// we can accept ID as a parameter too
			if (typeof resource == "number") {
				resource = new Resource(resource);
			}
			// default is true (when null is passed)
			if(children != false){children = true;}

			if (!resource.exists()) {
			    // import resource's parent as well if it's not already imported
			    var resParent = resource.parent(false);
			    if(resParent){
			        common.debug("Importing resources's parent");
			        DiscoveryBoss.importResources([resParent.getId()]);
			    }
				DiscoveryBoss.importResources([resource.getId()]);
				common.waitFor(resource.exists);
			}
			if (children) {
				common.debug("Importing resources's children");
				_importResources({parentResourceId:resource.getId()});
			}
			return resource;
		},
    /**
	 * imports all resources found in discovery queue
   * @param param - filter resources being imported similar to {@link resources.find()}
	 * @type Resource[]
	 * @function
	 * 	 * @example discovery.importResources() // import all resources in discovery queue
	 */
		importResources : _importResources
	};
}) ();

/**
 * creates a new instance of Resource
 * @class
 * @constructor
 * @param param {org.rhq.bindings.client.ResourceClientProxy|Number} proxy object or id 
 */
var Resource = function (param) {
	var common = new _common();
	common.trace("new Resource("+param+")");
	if (!param) {
		throw "either Number or org.rhq.bindings.client.ResourceClientProxy parameter is required";
	}
	common.debug("Retrieving proxy instance")
	if ("number" == typeof param) {
		param = ProxyFactory.getResource(param);
	}
	else {
		param = ProxyFactory.getResource(param.id);
	}

	var _id = param.id;
	var _name = String(param.name);
	var _res = param;
	
	
	
	// we define a metric as an internal type
	
	/**
	 * creates a new instance of Metric
	 * @class
	 * @constructor
	 * @name Metric
	 */
	var Metric = function(param,res) {
		var common = new _common();
		var _param = param;
		var _res = res;
		var _getMDef = function(){
			var criteria = common.createCriteria(new MeasurementDefinitionCriteria(),{resourceTypeId:_res.resourceType.id,displayName:_param.name});				
			var mDefs = MeasurementDefinitionManager.findMeasurementDefinitionsByCriteria(criteria);
			
			var index = -1
			for (i=0;i<mDefs.size();i++) {
				if (mDefs.get(i).displayName == _param.name) {
					index = i;
					break;
				}
			}
			if (index == -1) {
				throw "Unable to retrieve measurement definition, this is a bug"
			}
			return mDefs.get(index);
		};
		var _getSchedule = function(){
			var mDefId = _getMDef().id;
			common.trace("Retrieving schedules for resource with id: " +_res.id + 
					" and measurement definition id: " +mDefId);
			var criteria = common.createCriteria(new MeasurementScheduleCriteria(),
					{resourceId:_res.id,definitionIds:[mDefId]});				
			var schedules = MeasurementScheduleManager.findSchedulesByCriteria(criteria);
			
			if(schedules.size()==0){
				throw "Unable to retrive schedule for this Metric!!";
			}
			if(schedules.size()>1){
				throw "Retrived multiple schedules for this Metric!!";
			}			
			return schedules.get(0);
		};
		
		
		
		return {
			/**
			 * name of metric
			 * @lends Metric.prototype
			 * @field
			 * @type String
			 */
			name : param.name,
			/**
			 * data type of metric
			 * @field
			 * @type String
			 */
			dataType : String(param.dataType.name()),
			/**
			 * gets live value for metric
			 * @type String
			 * @returns String whatever the value is
			 * 
			 */
			getLiveValue : function() {
				common.trace("Resource("+_res.id+").metrics.["+param.name+"].getLiveValue()");				
				var defId = _getMDef().id;
				var values = MeasurementDataManager.findLiveData(_res.id,[defId]).toArray()
				// values is returned as set
				if (values.length>0) {
					return String(values[0].value);
				}
				common.info("No live value retrieved!");
				
			},
			/**
			 * enables/disables metric and sets its collection interval
			 * @param enabled {Boolean} - enable or disable metric
			 * @param interval {Number} - optinally set collection interval (seconds)
			 */
			set : function(enabled,interval) {
				common.trace("Resource("+_res.id+").metrics.["+param.name+"].set(enabled="+enabled+",interval="+interval+")");
				var defId = _getMDef().id;
				if (enabled==false) {
					common.debug("Disabling measurement");
					MeasurementScheduleManager.disableSchedulesForResource(_res.id,[defId])
					return;
				}else if (enabled==true) {
					common.debug("Enabling measurement");
					MeasurementScheduleManager.enableSchedulesForResource(_res.id,[defId])
				}
				if (typeof interval == "number") {
					common.debug("Setting collection interval to "+interval+"s");
					MeasurementScheduleManager.updateSchedulesForResource(_res.id,[defId],interval*1000);
				}
				
			},
			getScheduleId : function(){
				return new _getSchedule().getId();
			},
			/**
			 * gets actual metric collection interval
			 * @type Number
			 * @returns Number interval in milis
			 */
			getInterval : function(){
				return new Number(_getSchedule().getInterval());
			}, 
			/**
			 * returns true if this metrics is enabled, false otherwise
			 * @type Boolean
			 * @returns Boolean true if this metrics is enabled, false otherwise
			 */
			isEnabled : function(){
				return _getSchedule().isEnabled();
			}
		}
	};
	
  var _dynamic = {};
  
  var _shortenMetricName = function(name) {
	  return (String(name)[0].toLowerCase()+name.substring(1)).replace(/ /g,"");
  }
  
  /**
   * gets calltimes for given resource (Note that this method gets injected to resource object only when a resource has CALLTIME metric)
   * @function
   * @lends Resource.prototype
   * @returns array of simple data objects having fields same as org.rhq.core.domain.measurement.calltime.CallTimeDataComposite
   */
  var _getCallTimes = function(beginTime,endTime) {
	  common.trace("Resource("+_id+").getCallTimes(beginTime="+beginTime+",endTime="+endTime+")");
	  beginTime = beginTime || new Date().getTime() - (8 * 3600 * 1000);
	  endTime = endTime || new Date().getTime(); // 8 hours by default
	  for (_k in _metrics) {
		var metric = _metrics[_k]
		if (metric.dataType == "CALLTIME") {
			var callTimes = CallTimeDataManager.findCallTimeDataForResource(metric.getScheduleId(),beginTime,endTime,PageControl.getUnlimitedInstance());
			return common.pageListToArray(callTimes).map(function(x) {
				return {callDestination:x.callDestination, average:x.average, total:x.total, minimum:x.minimum, maximum:x.maximum, count: x.count};
			});
		}
	  }
	  throw "No CALLTIME Schedule found for this resource"
  }
  
  common.trace("Enumerating metrics")
  var _metrics = {};
  for (index in param.measurements) {
	  var metric = new Metric(param.measurements[index],param);
	  var _metricName = _shortenMetricName(metric.name);
	  _metrics[_metricName] = metric;
	  if (metric.dataType == "CALLTIME") {
		  _dynamic.getCallTimes = _getCallTimes; // inject _getCallTimes function
	  }
  }
  var _retrieveContent = function(destination) {
		var self = ProxyFactory.getResource(_id);
		var func = function() {
			try {
				self.retrieveBackingContent(destination);
				return true;
			} catch (e) {
				var msg = new java.lang.String(e);
				if (msg.contains("Please try again in a few minutes")) {
					common.debug("A known exception has been thrown when retrieving backing content, retrying");
					common.debug(e);
				} else {
					common.debug(e);
					throw e;
				}
			}
		};
		common.waitFor(func);
	};

	// initialize dynamic methods
	if (typeof(param.retrieveBackingContent) != "undefined") {
		// methods for updating/retrieving backing content are generated
		// dynamically only for content-based resources
		// workaround for https://bugzilla.redhat.com/show_bug.cgi?id=830841
		/**
		 * @function
		 * @lends Resource.prototype
		 */
		_dynamic.retrieveContent = _retrieveContent;
		_dynamic.updateContent = function (content,version) {
			var self = ProxyFactory.getResource(_id);
			var func = function() {
				try {
					self.updateBackingContent(content,version);
					return true;
				} catch (e) {
					var msg = new java.lang.String(e);
					if (msg.contains("Please try again in a few minutes")) {
						common.debug("A known exception has been thrown when updating backing content, retrying");
						common.debug(e);
					} else {
						common.debug(e);
						throw e;
					}
				}
			};
			common.waitFor(func);
		};
	}


	var _getName = function(){
		return String(_res.getName());
	}

	var _find = function() {
		var criteria = resources.createCriteria({id:_id});
		var res = ResourceManager.findResourcesByCriteria(criteria);
		// common.debug("Resource.find: "+resources);
		return res;
	};
	var _isAvailable = function() {
		common.trace("Resource("+_id+").isAvaialbe()");
		var found = _find();
		if (found.size() != 1) {
			return false;
		}
		return found.get(0).getCurrentAvailability().getAvailabilityType() == AvailabilityType.UP;
	};
	var _exists = function() {
		return _find().size() == 1;
	};
	var _parent = function(imported) {
		var criteria = resources.createCriteria({id:_id});
		criteria.fetchParentResource(true);
		if(imported == false){
		    criteria.addFilterInventoryStatus(InventoryStatus.NEW);
		}
		var res = ResourceManager.findResourcesByCriteria(criteria);
		if (res.size()==1 && res.get(0).parentResource) {
		    var par = res.get(0).parentResource;
		    if(imported == false){
			    if(par.getInventoryStatus() == InventoryStatus.NEW){
			        return new Resource(par.id);
			    }
			}else{
			    return new Resource(par.id);
			}
		}
	};

	var _waitForOperationResult = function(resOpShedule){
		var opHistCriteria = new ResourceOperationHistoryCriteria();
		if(resOpShedule)
			opHistCriteria.addFilterJobId(resOpShedule.getJobId());
		opHistCriteria.addFilterResourceIds(_id);
		opHistCriteria.addSortStartTime(PageOrdering.DESC); // put most recent
															// at top of results
		opHistCriteria.clearPaging();
		opHistCriteria.fetchResults(true);
		var pred = function() {
			var histories = OperationManager.findResourceOperationHistoriesByCriteria(opHistCriteria);
			if (histories.size() > 0) {
				if (histories.get(0).getStatus() != OperationRequestStatus.INPROGRESS) {
					return histories.get(0);
				}
				common.debug("Operation in progress..");
			};
		};
		common.debug("Waiting for result..");
		sleep(3000); // trying to workaround https://bugzilla.redhat.com/show_bug.cgi?id=855674
		var history = common.waitFor(pred);
		if (!history) {
			// timed out
			var histories = OperationManager.findResourceOperationHistoriesByCriteria(opHistCriteria);
			if (histories.size() > 0) {
				history = histories.get(0);
			}
			else {
				throw "ERROR Cannot get operation history result remote API ERROR?";
			}
		}
		common.debug("Operation finished with status : "+history.status);
		return history;
	};
	var _checkRequiredConfigurationParams = function(configDef,params) {
		if (!configDef) {
			return;
		}
		params = params || {};
		// check whether required params are defined
		var iter = configDef.getPropertyDefinitions().values().iterator();
		while(iter.hasNext()) {
			var propDef = iter.next();
			if (propDef.isRequired() && !params[propDef.name]) {
				throw "Property ["+propDef.name+"] is required";
			}
		}
	};
	var _checkOperationName = function(name){
		// let's obtain operation definitions, so we can check operation name
		var criteria = new ResourceTypeCriteria();
		criteria.addFilterId(_find().get(0).resourceType.id);
		criteria.fetchOperationDefinitions(true);
		var resType = ResourceTypeManager.findResourceTypesByCriteria(criteria).get(0);
		var iter = resType.operationDefinitions.iterator();
		// we put op names here in case invalid name is called
		var ops="";
		while(iter.hasNext()) {
			var operationDefinition = iter.next();
			ops+=operationDefinition.name+", ";
			if (name==operationDefinition.name) {
				return operationDefinition;	
			}
		}
		throw "Operation name ["+name+"] is invalid for this resource, valid operation names are : " + ops;	
	};
	var _createOperationConfig = function(params, operationDefinition){
		var configuration = null;
		if (params || params == {}) {
			configuration = common.hashAsConfiguration(params);
		}
		else if (operationDefinition.parametersConfigurationDefinition){
			var template = operationDefinition.parametersConfigurationDefinition.defaultTemplate;
			common.trace("Default template for parameters configuration definition" + template);
			if (template) {
				configuration = template.createConfiguration();
			}
		}
		// if (configuration)
		// pretty.print(configuration);
		// println(common.objToString(common.configurationAsHash(configuration)));
		
		return configuration;
	}


	var _static =  {
	  /**
    * gets resource ID
    * @type Number
	  * @lends Resource.prototype
	  */
    id : _id,
    /**
     * gets resource name
     * @type String
     */
    name : _name,
	/**
	 * enumerates metrics available for this resource, returned value is not an array but a hash,
	 * where name is metric name without spaces
	 * @example resource.metrics.totalSwapSpace // get's a metric called 'Total Swap Space'
	 * @type Metric[]
	 * @field
	 */
    metrics : _metrics,
    /**
     *  Update resource's editable properties (name, description, location).
     *  @example new Resource(12345).update({name:"new name",description:"new description",location:"new location"})
     *  @type Resource
     *  @return updated Resource
     */
    update : function(params) {
         var _self = _find().get(0);
         params.name = params.name || _self.name;
         params.description = params.description || _self.description;
         params.location = params.location || _self.location;

        _self.name = params.name;
        _self.description = params.description;
        _self.location = params.location;
        ResourceManager.updateResource(_self);
        return new Resource(_self);
    },
    /**
     * gets a metric by it's name
     * @example resource.getMetric("Total Swap Space")
     * @type Metric
     */
    getMetric : function(name) {
    	common.trace("Resource("+_id+").getMetric("+name+")");
    	var _k = _shortenMetricName(name);
    	if (_k in _metrics) {
    		return _metrics[_k];
    	}
    	else {
    		throw "Cannot find metric called ["+name+"]"
    	}
    },
    /**
    * gets resource ID
	  * @type Number
	  */
    
    getId : function() {return _id;},
    /**
    * gets resource String representation
    * @type String
    */
	toString : function() {return _res.toString();},
    /**
	  * gets resource name
	  * @type String
	  */
    getName : function() {return _getName();},
    /**
	  * gets resource type id
	  * @type Number
	  */
    getResourceTypeId : function(){
    	return _res.resourceType.id;
    },
	  /**
	  * @returns Resource proxy object
	  */
    getProxy : function() {
			common.trace("Resource("+_id+").getProxy()");
			return ProxyFactory.getResource(_id);
		},
    /**
	  * @param {Boolean} if set to true, we will search for parent resource with inventory status COMMITED
	  * false will search for parent resource with status NEW, default is true
	  * @returns parent resource
	  * @type Resource
	  */
		parent : function(imported) {
			common.trace("Resource("+_id+").parent("+imported+")");
			return _parent(imported);
		},
		/**
		 * removes/deletes this resource from inventory.
		 *
		 * @returns true if resource no longer exists in inventory, false
		 *          otherwise
		 * @type Boolean
		 */
		remove : function() {
			common.trace("Resource("+_id+").remove()");
			if (!_exists()) {
				common.debug("Resource does not exists, nothing to remove");
				return false;
			}
			var parent = _parent();
			if (!parent) {
				throw "Resource cannot be deleted without having parent";
			}
			var startTime = new Date().getTime();
			var parentId = parent.getId();
			try {
				var history = ResourceFactoryManager.deleteResource(_id);
			}
			catch (exc) {
				common.info("Resource was not deleted :"+exc);
				return false;
			}
			var pageControl = new PageControl(0,1);
			var pred = function() {
				var histories = ResourceFactoryManager.findDeleteChildResourceHistory(parentId,startTime,new Date().getTime(),pageControl);
				var current;
				common.pageListToArray(histories).forEach(
						function (x) {
							if (x.id==history.id) {
								if (x.status != DeleteResourceStatus.IN_PROGRESS) {
									current = x;
									return;
								}
								common.info("Waiting for resource to be removed");
							}
						}
				);
				return current;
			};
			var result = common.waitFor(pred);
			if (result) {
				common.debug("Resource deletion finished with status : "+result.status);
			}
			if (result && result.status == DeleteResourceStatus.SUCCESS) {
				common.debug("Resource was removed from inventory");
				return true;
			}
			if (!result) {
				common.info("Resource deletion still in progress, giving up...");
				return false;
			}
			common.info("Resource deletion failed, reason : "+result.errorMessage);
			return false;
		},
		/**
		 * get's child resources
		 * @param {Object} params - you can filter child resources same way as in {@link resources.find()} function
		 * @returns array of child resources
		 * @type Resource[]
		 */
		children : function(params) {
			common.trace("Resource("+_id+").children("+common.objToString(params)+")");
			params = params || {};
			params.parentResourceId=_id;
			return resources.find(params);
		},
		/**
		 * gets child resource by given params
		 * @param {Object} params - you can filter child resources same way as in {@link resources.find()} function
		 * @returns first matching child resource found
		 * @type Resource
		 */
		child : function(params) {
			common.trace("Resource("+_id+").child("+common.objToString(params)+")");
			params = params || {};
			params.parentResourceId=_id;
			var children = resources.find(params);
			if (children.length>0) {
				return children[0];
			}
		},
        	      /**
                     * wait's until given child resource exists (useful when you
                     * need to wait for discovery)
                     * 
                     * @param {Object} -
                     *            you can filter child resources same way as in
                     *            {@link resources.find()} function
                     * @returns first matchin child resource found
                     * @type Resource
                     */
        waitForChild : function(params) {
            common.trace("Resource(" + _id + ").waitForChild(" + common.objToString(params) + ")");
            return common.waitFor(function() {
                params = params || {};
                params.parentResourceId = _id;
                var children = resources.find(params);
                if (children.length == 0) {
                    common.info("Waiting for resource " + common.objToString(params));
                } else {
                    return children[0];
                }
            });
        },
		/**
		 * gets alert definitions for this resource
		 * @param {Object} params - you can filter alert definitions same as in {@link alertDefinitions.find()} function
		 * @returns array of alert definitions
		 * @type AlertDefinition[]
		 */
		alertDefinitons : function(params) {
			common.trace("Resource("+_id+").alertDefinitions("+common.objToString(params)+")");
			params = params || {};
			params["resourceIds"] = _id;
			return alertDefinitions.find(params);
		},
		/**
		 * updates configuration of this resource. You can either pass whole
		 * configuration (retrieved by {@link Resource.getConfiguration()}) or only params that
		 * needs to be changed
     * @example // to switch agent resource to ssl socket
     * agent.updateConfiguration({'rhq.communications.connector.transport':'sslsocket'});
		 * @param {Object} params - new configuration parameters, partial configuration is supported
		 * @returns True if configuration was updated
		 * @type Boolean
		 */
		updateConfiguration : function(params) {
			common.info("Updating configuration of resource with id: " + _id);
			common.trace("Resource("+_id+").updateConfiguration("+common.objToString(params)+")");
			params = params || {};
			common.debug("Retrieving configuration and configuration definition");
			var self = ProxyFactory.getResource(_id);
			var config = ConfigurationManager.getLiveResourceConfiguration(_id,false);
			common.debug("Got configuration : "+config +", "
					+ common.objToString(common.configurationAsHash(config)));
			var configDef = ConfigurationManager.getResourceConfigurationDefinitionForResourceType(self.resourceType.id);
			var applied = common.applyConfiguration(config,configDef,params);
			common.debug("Will apply this configuration: "+applied +", "
					+ common.objToString(common.configurationAsHash(applied)));

			var update = ConfigurationManager.updateResourceConfiguration(_id,applied);
			if (!update) {
				common.debug("Configuration has not been changed");
				return;
			}
			// TODO see https://bugzilla.redhat.com/show_bug.cgi?id=1020374
			if (update.status == ConfigurationUpdateStatus.INPROGRESS) {
				var pred = function() {
					var up = ConfigurationManager.getLatestResourceConfigurationUpdate(_id);
					if (up) {
						return up.status != ConfigurationUpdateStatus.INPROGRESS;
					}
				};
				common.debug("Waiting for configuration to be updated...");
				var result = common.waitFor(pred);
				if (!result) {
					throw "Resource configuration update timed out!";
				}
				update = ConfigurationManager.getLatestResourceConfigurationUpdate(_id);
			}
			common.debug("Configuration update finished with status : "+update.status);
			if (update.status == ConfigurationUpdateStatus.FAILURE) {
				common.info("Resource configuration update failed : "+update.errorMessage);
			}
			else if (update.status == ConfigurationUpdateStatus.SUCCESS) {
				common.info("Resource configuration was updated");
			}
			
			return update.status == ConfigurationUpdateStatus.SUCCESS;
		},
		/**
		 * retrieves LIVE configuration of this resource
		 *
		 * @returns live configuration or null if it is not available (ressource is DOWN or UNKNOWN)
		 * @type Object
		 */
		getConfiguration : function() {
			common.trace("Resource("+_id+").getConfiguration()");
			var self = ProxyFactory.getResource(_id);
			var configDef = ConfigurationManager.getResourceConfigurationDefinitionForResourceType(self.resourceType.id);
			return common.configurationAsHash(ConfigurationManager.getLiveResourceConfiguration(_id,false),configDef);
		},
		/**
		 * updates plugin configuration (Connection settings) of this resource.  You can either pass whole
		 * configuration (retrieved by {@link Resource.getPluginConfiguration()}) or only params that
		 * needs to be changed
		 * @example // set new start script arguments for as7
		 * as7.updatePluginConfiguration({'startScriptArgs':'-Djboss.server.base.dir=foo'});
		 * @param {Object} params - new configuration parameters, partial configuration is supported
		 * @returns True if configuration was updated
		 * @type Boolean
		 */
		updatePluginConfiguration : function(params) {
			common.info("Updating plugin configuration of resource with id: " + _id);
			common.trace("Resource("+_id+").updatePluginConfiguration("+common.objToString(params)+")");
			params = params || {};
			common.debug("Retrieving plugin configuration and configuration definition");
			var self = ProxyFactory.getResource(_id);
			var config = ConfigurationManager.getPluginConfiguration(_id);
			common.debug("Got configuration : "+config +", "+ common.objToString(common.configurationAsHash(config)));
			var configDef = ConfigurationManager.getPluginConfigurationDefinitionForResourceType(self.resourceType.id);
			var applied = common.applyConfiguration(config,configDef,params);
			common.debug("Will apply this configuration: "+applied +", " + common.objToString(common.configurationAsHash(applied)));

			var update = ConfigurationManager.updatePluginConfiguration(_id,applied);
			if (!update) {
				common.debug("Configuration has not been changed");
				return;
			}
			if (update.status == ConfigurationUpdateStatus.INPROGRESS) {
				var pred = function() {
					var up = ConfigurationManager.getLatestPluginConfigurationUpdate(_id);
					if (up) {
						return up.status != ConfigurationUpdateStatus.INPROGRESS;
					}
				};
				common.debug("Waiting for configuration to be updated...");
				var result = common.waitFor(pred);
				if (!result) {
					throw "Resource configuration update timed out!";
				}
				update = ConfigurationManager.getLatestPluginConfigurationUpdate(_id);
			}
			common.debug("Configuration update finished with status : "+update.status);
			if (update.status == ConfigurationUpdateStatus.FAILURE) {
				common.info("Resource configuration update failed : "+update.errorMessage);
			}
			else if (update.status == ConfigurationUpdateStatus.SUCCESS) {
				common.info("Resource configuration was updated");
			}
			return update.status == ConfigurationUpdateStatus.SUCCESS;
		},
		/**
		 * retrieves plugin configuration for this resource
		 *
		 * @returns plugin configuration object (Connection settings)
		 * @type Object
		 */
		getPluginConfiguration : function() {
			common.trace("Resource("+_id+").getPluginConfiguration()");
			var self = ProxyFactory.getResource(_id);
			var configDef = ConfigurationManager.getPluginConfigurationDefinitionForResourceType(self.resourceType.id);
			return common.configurationAsHash(ConfigurationManager.getPluginConfiguration(_id),configDef);
		},
		/**
		 * creates a new child resource
		 *
		 * @param {Object} params can contain following: -
		 * <ul>
		 * <li>name [String] (required)- name for a new resource child, name is optional when `content` is provided</li>
		 * <li>type [String] (required) - resource type name to be created (exact name, not just filter)</li>
		 * <li>config [Object] (optional) - configuration map for new resource, if not present, default is taken</li>
		 * <li>pluginConfig [Object] (optional) - plugin configuration for new resource, if not present, default is taken (NOT YET IMPLEMENTED)</li>
		 * <li>content [String] (optional) - absolute path for resource's content file (typical for deployments)</li>
		 * <li>version [String] (optional) - version string</li>
		 * @example // create a deployment child on JBoss AS7
     * as.createChild({name:"hello.war",type:"Deployment",content:"/tmp/hello.war"});
		 * @example // create a network interface with configuration on JBoss AS7
     * as.createChild({name:"testinterface",type:"Network Interface",config:{"inet-address":"127.0.0.1","any-address":false}});
     * @returns new resource if it was successfully created and discovered, null otherwise
		 * @type Resource
		 */
		createChild : function(params) {
			common.trace("Resource("+_id+").createChild("+common.objToString(params)+")");
			params = params || {};
			if (!params.name && !params.content) {
				throw "Either [name] or [content] parameters must be defined";
			}
			if (!params.type) {
				throw "[type] parameter MUST be specified, how could I guess what type of resource are you creating?";
			}
			// TODO check for existing resource!
			var name=params.name;
			if (!params.name) {
				name = new java.io.File(params.content).getName();
			}
			// bind input params
			var type = params.type;
			var config = params.config;
			var version = params.version || null;
			var content = params.content;
			// these 2 are used for querying resource history
			var startTime = new Date().getTime();
			var pageControl = new PageControl(0,1);
			// we need to obtain resourceType for a new resource
			var selfType = _find().get(0).resourceType;
			var criteria = common.createCriteria(new ResourceTypeCriteria(),{name:type,pluginName:selfType.plugin,parentId:selfType.id,createDeletePolicy:CreateDeletePolicy.BOTH});
			criteria.fetchResourceConfigurationDefinition(true);
			criteria.fetchPluginConfigurationDefinition(true);
			var resTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);
			var failed = resTypes.size() == 0;
			var resType = null;
			for (var i=0;i<resTypes.size();i++) {
				if (resTypes.get(i).name == type) {
					resType = resTypes.get(i);
					failed = false;
					break;
				}
			}
			if (failed)  {
				criteria = common.createCriteria(new ResourceTypeCriteria(),{pluginName:selfType.plugin,parentId:selfType.id,createDeletePolicy:CreateDeletePolicy.BOTH});
				resTypes = ResourceTypeManager.findResourceTypesByCriteria(criteria);
				var types = "";
				for (var i=0;i<resTypes.size();i++) {
					types+=resTypes.get(i).name+", ";
				}
				throw "Invalid resource type [type="+type+"] valid type names are ["+types+"]";
			}
			var configuration =  new Configuration();
		    if (config) {
		    	configuration = common.hashAsConfiguration(config);
		    }
		    else {
		    	// we should obtain default/empty configuration
		    	common.debug("No configuration passed, using default");
		    	var template = resType.resourceConfigurationDefinition.defaultTemplate;
				if (template) {
					configuration = template.createConfiguration();
				}
		    }
			common.debug("Creating new ["+type+"] resource called [" + name+"] with following configuration: ["
					+ common.objToString(common.configurationAsHash(configuration)) + "]");
			if (content) {
				// we're creating a resource with backing content
				var file = new java.io.File(content);
				if (!file.exists()) {
					throw "content parameter file '" +content+ "' does not exist!";
				}
				if (typeof scriptUtil.uploadContent !== "undefined") {
				    var handle = scriptUtil.uploadContent(content);
				    var history = ResourceFactoryManager.createPackageBackedResourceViaContentHandle(
	                        _id,
	                        resType.id,
	                        name, // new resource name
	                        null, // pluginConfiguration
	                        name,
	                        version, // packageVersion
	                        null, // architectureId
	                        configuration, // resourceConfiguration
	                        handle, // content
	                        null // timeout
	                    );
				}
				else {
				    // keep for compatibility with < JON 3.2
    				common.debug("Reading file " + content + " ...");
    			    var inputStream = new java.io.FileInputStream(file);
    			    var fileLength = file.length();
    			    var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
    			    for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
    				    numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset);
    			    }
    
    				var history = ResourceFactoryManager.createPackageBackedResource(
    					_id,
    					resType.id,
    					name, // new resource name
    					null, // pluginConfiguration
    					name,
    					version, // packageVersion
    					null, // architectureId
    					configuration, // resourceConfiguration
    					fileBytes, // content
    					null // timeout
    				);
				}
			}
			else {
				var plugConfiguration = new Configuration();
				var pluginTemplate = resType.pluginConfigurationDefinition.defaultTemplate;
				if (pluginTemplate) {
					plugConfiguration = pluginTemplate.configuration;
				}
				var history = ResourceFactoryManager.createResource(
					_id,
					resType.id,
					name, // new resource name
					plugConfiguration, // pluginConfiguration
					configuration, // resourceConfiguration
					null  // timeout
				);
			}
			var pred = function() {
				var actualDateMilis = new Date().getTime();
				common.trace("Searching for CreateChildResourceHistory. Res id: "+_id+", startTime: "+new Date(startTime)+
						", actualDateMilis: "+new Date(actualDateMilis));
				var histories = ResourceFactoryManager.findCreateChildResourceHistory(_id,startTime,actualDateMilis,pageControl);
				//TODO check this on other relevant parts or use time from server
				if(histories.size() == 0){
					common.warn("No history found inside given range. The cause of this could be that time" +
							" on machine witch CLI client and on machine with RHQ server is not synchronised.");
				}
				var current;
				common.pageListToArray(histories).forEach(
						function (x) {
							if (history && x.id==history.id) {
								if (x.status != CreateResourceStatus.IN_PROGRESS) {
									current = x;
									return;
								}
								common.info("Waiting for resource creation..");
							}
						}
				);
				return current;
			};
			common.debug("Waiting for resrouce creation operation...");
			var result = common.waitFor(pred);
			if (result) {
				common.debug("Child resource creation status : " + result.status);
			}
			else {
				common.info("Child resource creation timed out!!");
				return;
			}
			if (result && result.status == CreateResourceStatus.SUCCESS) {
				common.debug("Waiting for resource to be auto-discovered");
				// we assume there can be exactly one resource of one type
				// having unique name
				var discovered = common.waitFor(function() {return resources.find({parentResourceId:_id,resourceTypeId:resType.id,resourceKey:result.newResourceKey}).length==1;});
				if (!discovered) {
					common.info("Resource child was successfully created, but it's autodiscovery timed out!");
					return;
				}
				else {
					common.info("Resource child was successfully created");
				}
				return resources.find({parentResourceId:_id,resourceTypeId:resType.id,resourceKey:result.newResourceKey})[0];
			}
			common.debug("Resource creation failed, reason : "+result.errorMessage);
			return;

		},
		operations : {
        test1 : {
          run : function(params) {
          
          }
        }

		},
		/**
		 * invokes operation on resource, operation status is polled 'till
		 * timeout is reached or operation finishes
		 *
		 * @param {String}
		 *            name of operation (required)
		 * @param {Object}
		 *            params - hashmap for operation params (Configuration) (optional)
		 * @type {Object}
		 * @returns javascript object (hashmap) with following keys:
		 * <ul>
		 * <li>status {String} - operation status<li>
		 * <li>error {String} - operation error message</li>
		 * <li>result {Object} -  result configuration</li>
		 * <ul>
		 */
		invokeOperation : function(name,params) {
			common.trace("Resource("+_id+").invokeOperation(name="+name+",params={"+common.objToString(params)+"})");
			// let's obtain operation definitions, so we can check operation
			var op = _checkOperationName(name);
			var configuration = _createOperationConfig(params,op);
			_checkRequiredConfigurationParams(op.parametersConfigurationDefinition,common.configurationAsHash(configuration));

			var resOpShedule = OperationManager.scheduleResourceOperation(_id,name,0,0,0,0,configuration,null);
			common.info("Operation ["+name+"] scheduled");
			var result = _waitForOperationResult(resOpShedule);
			var ret = {}
			ret.status = String(result.status)
			ret.error = String(result.errorMessage)
			ret.result = common.configurationAsHash(result.results,op.resultsConfigurationDefinition);
			return ret;
		},
		/**
		 * schedules operation on resource. In contrast to invokeOperation this is
		 * not blocking (synchronous) operation.
		 *
		 * @param {String}
		 *            name of operation (required)
		 *            
		 * @param {long} delay delay in seconds (required)
		 * @param {long} repeatInterval repeatInterval in seconds (required)
		 * @param {int} repeatCount repeatCount in seconds (required)
		 * @param {Object}
		 *            opParams - hashmap for operation params (Configuration) (optional)
		 */
		scheduleOperation : function(name,delay,repeatInterval,repeatCount,opParams) {
			common.trace("Resource("+_id+").scheduleOperation(name="+name+", delay="
					+delay+", repeatInterval="+repeatInterval+", repeatCount="+repeatCount+
					", opParams={"+common.objToString(opParams)+"})");
			if(delay < 0){throw "Delay of scheduled operation must be >= 0 !!!";}
			if(repeatInterval < 0){throw "Repeat interval of scheduled operation must be >= 0 !!!";}
			if(repeatCount < 0){throw "Repeat count of scheduled operation must be >= 0 !!!";}
			
			// let's obtain operation definitions, so we can check operation
			var op = _checkOperationName(name);
			var configuration = _createOperationConfig(opParams,op);
			_checkRequiredConfigurationParams(op.parametersConfigurationDefinition,common.configurationAsHash(configuration));

			var resOpShedule = OperationManager.scheduleResourceOperation(_id,name,delay * 1000,
					repeatInterval * 1000,repeatCount,0,configuration,null);
			common.info("Operation '"+name+"' scheduled");
		},
		/**
		 * schedules operation on resource using crone expression. In contrast to invokeOperation this is 
		 * not blocking (synchronous) operation.
		 *
		 * @param {String}
		 *            name of operation (required)
		 *            
		 * @param {String} cronExpression (required)
		 * @param {Object}
		 *            opParams - hashmap for operation params (Configuration) (optional)
		 */
		scheduleOperationUsingCron : function(name,cronExpression,opParams) {
			common.trace("Resource("+_id+").scheduleOperationUsingCron(name="+name+", cronExpression="+cronExpression+
					", opParams={"+common.objToString(opParams)+"})");
			
			// let's obtain operation definitions, so we can check operation
			var op = _checkOperationName(name);
			var configuration = _createOperationConfig(opParams,op);
			_checkRequiredConfigurationParams(op.parametersConfigurationDefinition,common.configurationAsHash(configuration));
			
			var resOpShedule = OperationManager.scheduleResourceOperationUsingCron(_id,name,cronExpression,0,configuration,null);
			common.info("Operation '"+name+"' scheduled");
		},
		/**
		 * Waits until operation is finished or timeout is reached.
		 *
		 * @param resOpShedule
		 *            may be null, than the most recent job for this resource
		 *            is picked
		 * @returns operation history
		 * @function
		 */
		waitForOperationResult : _waitForOperationResult,
		/**
		 * Checks that given operation with given parameters is valid on this resource.
		 * Default parameters are used when ommited.
		 *
		 * @param {String}
		 *            name of operation (required)
		 *            
		 * @param {Object}
		 *            opParams - hashmap for operation params (Configuration) (optional)
		 * @returns checked configuration
		 */
		checkOperation : function(name, opParams){
			var op = _checkOperationName(name);
			var configuration = _createOperationConfig(opParams,op);
			var confAsHash = common.configurationAsHash(configuration);
			_checkRequiredConfigurationParams(op.parametersConfigurationDefinition,confAsHash);
			return confAsHash;
		},
		/**
		 * checks whether resource exists in inventory
		 *
		 * @returns bool
		 * @type Boolean
		 */
		exists : function() {
			common.trace("Resource("+_id+").exists()");
			return _exists();
		},
		/**
		 * returns true if availability == UP
		 *
		 * @function
		 * @type Boolean
		 */
		isAvailable : _isAvailable,
		/**
		 * wait's until resource becomes UP or timeout is reached
		 *
		 * @returns true if resource became or is available, false otherwise
		 * @type Boolean
		 */
		waitForAvailable : function() {
			common.trace("Resource("+_id+").waitForAvailable()");
			return common.waitFor(function() {
				if (!_isAvailable()) {
					common.info("Waiting for resource availability=UP");
				} else { return true; }
			});
		},
		/**
		 * wait's until resource becomes DOWN or timeout is reached
		 *
		 * @returns true if resource became or is DOWN, false otherwise
		 * @type Boolean
		 */
		waitForNotAvailable : function() {
			common.trace("Resource("+_id+").waitForNotAvailable()");
			return common.waitFor(function() {
				if (_isAvailable()) {
					common.debug("Waiting for resource availability=DOWN");
				} else { return true; }
			});
		},
		/**
		 * unimports resource
		 *
		 * @returns true if resource is dos no longer exist in inventory, false
		 *          otherwise
		 * @type Boolean
		 */
		uninventory : function() {
			common.trace("Resource("+_id+").uninventory()");
			
			// this is a workaround for https://bugzilla.redhat.com/show_bug.cgi?id=830158
			var result = common.waitFor(function (){
				try{
					ResourceManager.uninventoryResources([_id]);
					return true;
				}catch(err){
					var errMsg = err.message;
					common.warn("Caught following error during uninventory: " + errMsg);
					if(errMsg.indexOf("Failed to uninventory platform. " +
							"This can happen if new resources were actively being imported. " +
							"Please wait and try again shortly") != -1){
						return false;
					}else{
						return -1;
					}
				}
			},20,480);
			
			if(!result || result == -1){
				throw "Failed to uninventory. See previous errors."
			}
			
			
			result = common.waitFor(function () {
					if (_find().size()>0) {
						common.debug("Waiting for resource to be removed from inventory");
						return false;
					}
					else {
						return true;
					}
				}
			);
			common.debug("Waiting 5s for sync..");
			sleep(5*1000);
			if (result) {
				common.info("Resource was removed from inventory");
			}
			return result;
		}
	};

	// merge dynamic methods into static ones
	for (_k in _dynamic) {
		_static[_k] = _dynamic[_k];
	}
	return _static;
};



/**
 * @lends _global_
 */
var Inventory = resources;
Inventory.discoveryQueue = discoveryQueue;

/**
 * verbosity, default 0 (-2=ERROR, -1=WARN, 0=INFO, 1=DEBUG, 2=TRACE)
 */
var verbose = 0;
/**
 * poll interval for any waiting in seconds
 */
var delay = 5;
/**
 * total timeout of any waiting in seconds
 */
var timeout = 360;

/**
 *  initializes verbosity and timeouts
 *  @param verb - verbosity
 *  @param dlay - delay
 *  @param tout - total timeout
 */
initialize = function(verb,dlay,tout) {
		verbose = verb;
		delay = dlay;
		timeout = tout;
		println("rhqapi initialized: verbose="+verb+" delay="+dlay+"s timeout="+tout+"s");
	}
// commonjs support
if (typeof exports !== "undefined") {
	exports.resources = resources;
	exports.discoveryQueue = discoveryQueue;
	exports.bundles = bundles;
    exports.bundleGroups = bundleGroups;
	exports.groups = groups;
	exports.Resource = Resource;
	exports.roles = roles;
	exports.users = users;
	exports.permissions = permissions;
	exports.alertDefinitions = alertDefinitions;
	exports.dynaGroupDefinitions = dynaGroupDefinitions;
	exports.resourceTypes = resourceTypes;
  exports.initialize = initialize;
}

// END of rhqapi.js

