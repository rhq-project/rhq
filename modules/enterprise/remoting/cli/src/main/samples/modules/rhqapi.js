/**
 * @overview this library tries to be synchronous and high-level API built on top of standard RHQ remote API
 * @name RHQ API
 * @version 0.2
 * @author Libor Zoubek (lzoubek@redhat.com), Filip Brychta (fbrychta@redhat.com)
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
		for(key in hash) {
			if (!hash.hasOwnProperty(key)) {
				continue;
			}
			value = hash[key];

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
							// me(prop, key, v);
						}
					}
				} else if (isHash(value)) {
					prop = new PropertyMap(key);
					for(var i in value) {
						var v = value[i];
						if (value != null) {
							// me(prop, i, v);
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
			})(config, key, value);
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
							representation = Number(prop.doubleValue);
						} else {
							representation = String(prop.stringValue);
						}
					}
					else {
						representation = String(prop.stringValue);
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
		for (var k in values) {
			// we only iterrate over values
			if (values.hasOwnProperty(k)) {
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

					if (propDef==null) {
						_debug("Unable to get PropertyDefinition for key="+key);
						return;
					}
					// process all 3 possible types
					if (propDef instanceof PropertyDefinitionSimple) {
						prop = new PropertySimple(key, null);

						if (value!=null) {
							prop = new PropertySimple(key, new java.lang.String(value));
						}
							// println("it's simple! "+prop);
					} else if (propDef instanceof PropertyDefinitionList) {
						prop = new PropertyList(key);
						// println("it's list! "+prop);
						for(var i = 0; i < value.length; ++i) {
							arguments.callee(prop,propDef,"",value[i]);
						}
					} else if (propDef instanceof PropertyDefinitionMap) {
						prop = new PropertyMap(propDef.name);
						// println("it's map! "+prop);
						for (var i in value) {
							if (value.hasOwnProperty(i)) {
								arguments.callee(prop,propDef,i,value[i]);
							}
						}
					}
					else {
						common.info("Unkonwn property definition! this is a bug");
						pretty.print(propDef);
						return
					}
					// now we update our Configuration node
					if (parent instanceof PropertyList) {
						parent.add(prop);
					} else {
						parent.put(prop);
					}
				}) (original,definition,k,values[k]);
			}
		}
		return original;
	};

	return {
		objToString : function(hash) {
			function isArray(obj) {
				return typeof (obj) == 'object' && (obj instanceof Array);
			}
			function isHash(obj) {
				return typeof (obj) == 'object' && !(obj instanceof Array);
			}

			function isPrimitive(obj) {
				return typeof (obj) != 'object' || obj == null || (obj instanceof Boolean || obj instanceof Number || obj instanceof String);
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
			for (key in hash) {
				if (!hash.hasOwnProperty(key)) {
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
						// primitive types
						if (value instanceof Number || value instanceof Boolean) {
							prop = kkey + value;
						} else {
							prop = kkey + "\'" + value + "\'";
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
				})(key, hash[key])

				if (valueStr) {
					output += valueStr + ",";
				}
			}
			output = output.substring(0, output.length - 1);
			return "{"+output+"}";
		},
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
			var resourcesArray = new Array();
		    var i = 0;
		    for(i = 0;i < pageList.size(); i++){
		    	resourcesArray[i] = pageList.get(i);
		    }
		    return resourcesArray;
		},
		/**
		 * @param conditionFunc -
		 *            predicate waits until conditionFunc does return any
		 *            defined value except for false
		 */
		waitFor : function(conditionFunc) {
			var time = 0;
			if (typeof timeout == "number") {
				var tout = timeout;
			}
			else {
				tout = 20;
			}
			if (typeof delay == "number") {
				var dlay = delay;
			}
			else {
				dlay = 5;
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
			for (var k in params) {
			    // use hasOwnProperty to filter out keys from the
				// Object.prototype
			    if (params.hasOwnProperty(k)) {
			    	if (shortcutFunc) {
				    	var shortcutExpr = shortcutFunc(k,params[k]);
				    	if (shortcutExpr) {
				    		// shortcut func returned something so we can eval
							// it and skip normal processing for this property
				    		eval("criteria."+shortcutExpr);
				    		continue;
				    	}
			    	}
			        var key = k[0].toUpperCase()+k.substring(1);
			        var func = eval("criteria.addFilter"+key);
			        if (typeof func !== "undefined") {
			        	func.call(criteria,params[k]);
			        }
			        else {
			        	var names = "";
			        	criteria.getClass().getMethods().forEach( function (m) {
			        		if (m.getName().startsWith("addFilter")) {
			        		 var name = m.getName().substring(9);
			        		 names+=name.substring(0,1).toLowerCase()+name.substring(1)+", ";
			        		}
			        	});
			        	throw "Parameter ["+k+"] is not valid filter parameter, valid filter parameters are : "+names;
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
		        var key = k[0].toUpperCase()+k.substring(1);
	        	var func = eval("natRole.set"+key);
	        	if(typeof func == "undefined"){
		        	throw "Given parameter '"+key+"' is not defined on org.rhq.core.domain.authz.Role object";
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
	nativeRole = nativeRole || {};
	common.debug("Creating an abstract role: " + nativeRole);
	
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
		        var key = k[0].toUpperCase()+k.substring(1);
		        var func = eval("subject.set"+key);
		        if(typeof func == "undefined"){
		        	throw "Given parameter '"+key+"' is not defined on Subject object";
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
	nativeSubject = nativeSubject || {};
	common.debug("Creating following abstract user: " + nativeSubject );
	
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
		 * get resources contained in this group
		 * @param params - you can filter child resources same way as in {@link resources.find()} function
		 * @returns array of resources
		 * @type Resrouce[]
		 */
		resources : function(params) {
			params = params || {};
			params.explicitGroupIds = [_id];
			return resources.find(params);
		},
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

  return {
		/**
		 * creates BundleCriteria object based on given params
		 *
		 * @param {Object} params - filter parameters
		 * @ignore
		 */
	  	createCriteria : function(params) {
			params = params || {};
			common.trace("bundles.createCriteria("+common.objToString(params) +")");
			var criteria = common.createCriteria(new BundleCriteria(),params);
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
    find : _find,
		/**
		 * creates a bundle
		 *
		 * @param {String} dist - path to bundle distribution ZIP file or URL. 
		 * If URL it must be reachable by RHQ server
		 * @type Bundle
		 */
    createFromDistFile : function(dist) {
    	if (dist==null) {
    		throw "parameter dist must not be null"
    	}
    	if (dist.indexOf("http")==0) {
    		var version = BundleManager.createBundleVersionViaURL(dist);
		    return new Bundle(version.bundle);
    	}
    	else {
			var file = new java.io.File(dist);
			if (!file.exists()) {
				throw "file parameter ["+file+"] does not exist!";
			}
		    var inputStream = new java.io.FileInputStream(file);
		    var fileLength = file.length();
		    var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
		    for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
			    numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset);
		    }
		    var version = BundleManager.createBundleVersionViaByteArray(fileBytes);
		    return new Bundle(version.bundle);
    	}
	},

		// createFromRecipe : function(recipe,files) {
			// we're creating a resource with backing content
			// common.debug("Reading recipe file " + recipe + " ...");
			// var file = new java.io.File(recipe);
			// if (!file.exists()) {
			// throw "recipe parameter file does not exist!";
			// }
		  // var inputStream = new java.io.FileInputStream(file);
		  // var fileLength = file.length();
		    // TODO read recipe to String properly!
		  // var fileBytes =
			// java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE,
			// fileLength);
		  // for (numRead=0, offset=0; ((numRead >= 0) && (offset <
			// fileBytes.length)); offset += numRead ) {
			// numRead = inputStream.read(fileBytes, offset, fileBytes.length -
			// offset);
		  // }
		  // println(fileBytes);
		  // var recipeStr = new String(fileBytes,0,fileLength);
		  // println(recipeStr);
		  // var bundleVersion =
			// BundleManager.createBundleVersionViaRecipe(recipeStr);
		// }
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
			 * removes this version of bundle from server (not yet implemented)
			 */
			remove : function() {

			},
			/**
			 * returns all files contained in this version of bundle (not yet implemented)
			 */
			files : function() {

			},
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
	 * @type Bundle-Deployment
	 */
		deploy : function(destination,params,version) {
			params = params || {};
			common.trace("Bundle("+_id+").deploy(destination="+destination+",params="+common.objToString(params)+",version="+version+")");
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
			println(BundleManager.findBundleVersionsByCriteria(criteria));
			version.obj = BundleManager.findBundleVersionsByCriteria(criteria).get(0);
			var configuration = new Configuration();
			// so if the bundle has come configuration, we create default
			// instance of it and apply our param values
			if (version.obj.configurationDefinition.defaultTemplate!=null) {
				var defaultConfig = version.obj.configurationDefinition.defaultTemplate.createConfiguration();
				configuration = common.applyConfiguration(defaultConfig,version.obj.configurationDefinition,params);
			}
			var deployment = BundleManager.createBundleDeployment(version.obj.id, destination.obj.id, "", configuration);
			deployment = BundleManager.scheduleBundleDeployment(deployment.id, true);
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
		common.trace("discoveryQueue._importResources("+common.objToString(params)+")");
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
	    assertTrue(committed.size() > 0, "COMMITED resources size > 0");
	    // return only imported resources
	    return common.pageListToArray(res).map(function(x){return new Resource(x);});
	};
  _listPlatforms = function(params) {
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
		importPlatform: function(name,children) {
			common.trace("discoveryQueue.importPlatform(name="+name+" children[default=true]="+children+")");

			// default is true (when null is passed)
			if(children != false){children = true;}

			// first lookup whether platform is already imported
			var reso = resources.find({name:name,category:"PLATFORM"});
			if (reso.length == 1) {
				common.debug("Platform "+name+" is already in inventory, not importing");
				return res[0];
			}
      if (_listPlatforms({name:name}).length < 1) {
        throw "Platform ["+name+"] was not found in discovery queue"
      }
			res = _importResources({name:name,category:"PLATFORM"});
			if (res.length != 1) {
        throw "Plaform was not imported, server error?"
      }
			if (children) {
				common.debug("Importing platform's children");
				_importResources({parentResourceId:res[0].getId()});
			}
			common.debug("Waiting 15 seconds, 'till inventory syncrhonizes with agent");
			sleep(15*1000);
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
	 */
		importResources : _importResources,
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
	var _name = param.name;
	var _res = param;
	
	// we define a metric as an internal type
	//TODO implement reading last metric value properly
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
		var _defId = function() {
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
			return mDefs.get(index).id;
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
			 * gets live value for metric
			 * @type String
			 * @returns String whatever the value is
			 * 
			 */
			getLiveValue : function() {
				common.trace("Resource("+_res.id+").metrics.["+param.name+"].getLiveValue()");				
				var defId = _defId();
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
				var defId = _defId();
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
				
			}
		}
	};
	
  var _dynamic = {};
  
  var _shortenMetricName = function(name) {
	  return (String(name)[0].toLowerCase()+name.substring(1)).replace(/ /g,"");
  }
  common.debug("Enumerating metrics")
  var _metrics = {};
  for (index in param.measurements) {
	  var metric = new Metric(param.measurements[index],param);
	  var _metricName = _shortenMetricName(metric.name);
	  _metrics[_metricName] = metric;
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
		return _res.getName();
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
	var _parent = function() {
		var criteria = resources.createCriteria({id:_id});
		criteria.fetchParentResource(true);
		var res = ResourceManager.findResourcesByCriteria(criteria);
		if (res.size()==1 && res.get(0).parentResource) {
			return new Resource(res.get(0).parentResource.id);
		}
	};

	var _waitForOperationResult = function(resourceId, resOpShedule){
		var opHistCriteria = new ResourceOperationHistoryCriteria();
		if(resOpShedule)
			opHistCriteria.addFilterJobId(resOpShedule.getJobId());
		opHistCriteria.addFilterResourceIds(resourceId);
		opHistCriteria.addSortStartTime(PageOrdering.DESC); // put most recent
															// at top of results
		opHistCriteria.setPaging(0, 1); // only return one result, in effect the
										// latest
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
     * gets a metric by it's name
     * @example resource.getMetric("Total Swap Space")
     * @type Metric
     */
    getMetric : function(name) {
    	common.trace("Resource("+_id+").getMetric("+name+")");
    	var key = _shortenMetricName(name);
    	if (key in _metrics) {
    		return _metrics[key];
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
	  * @returns Resource proxy object
	  */
    getProxy : function() {
			common.trace("Resource("+_id+").getProxy()");
			return ProxyFactory.getResource(_id);
		},
    /**
	  * @returns parent resource
	  * @type Resource
	  */
		parent : function() {
			common.trace("Resource("+_id+").parent()");
			return _parent();
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
			common.trace("Resource("+_id+").updateConfiguration("+common.objToString(params)+")");
			params = params || {};
			common.debug("Retrieving configuration and configuration definition");
			var self = ProxyFactory.getResource(_id);
			var config = ConfigurationManager.getLiveResourceConfiguration(_id,false);
			common.debug("Got configuration : "+config);
			var configDef = ConfigurationManager.getResourceConfigurationDefinitionForResourceType(self.resourceType.id);
			var applied = common.applyConfiguration(config,configDef,params);
			common.debug("Will apply this configuration: "+applied);

			var update = ConfigurationManager.updateResourceConfiguration(_id,applied);
			if (!update) {
				common.debug("Configuration has not been changed");
				return;
			}
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
		 * retrieves plugin configuration for this resource
		 *
		 * @returns
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
		    	var template = resType.resourceConfigurationDefinition.defaultTemplate;
				if (template) {
					configuration = template.createConfiguration();
				}
		    }
			common.debug("Creating new ["+type+"] resource called [" + name+"]");
			if (content) {
				// we're creating a resource with backing content
				common.debug("Reading file " + content + " ...");
				var file = new java.io.File(content);
				if (!file.exists()) {
					throw "content parameter file '" +content+ "' does not exist!";
				}
			    var inputStream = new java.io.FileInputStream(file);
			    var fileLength = file.length();
			    var fileBytes = java.lang.reflect.Array.newInstance(java.lang.Byte.TYPE, fileLength);
			    for (numRead=0, offset=0; ((numRead >= 0) && (offset < fileBytes.length)); offset += numRead ) {
				    numRead = inputStream.read(fileBytes, offset, fileBytes.length - offset);
			    }

				history = ResourceFactoryManager.createPackageBackedResource(
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
			var result = _waitForOperationResult(_id,resOpShedule);
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
		 * @returns
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
			common.info("Operation scheduled");
		},
		/**
		 * Waits until operation is finished or timeout is reached.
		 *
		 * @param resourceId
		 * @param resOpShedule
		 *            may be null, than the most recent job for given resourceId
		 *            is picked
		 * @returns operation history
		 * @function
		 */
		waitForOperationResult : _waitForOperationResult,
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
			ResourceManager.uninventoryResources([_id]);
			var result = common.waitFor(function () {
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
	for (key in _dynamic) {
		_static[key] = _dynamic[key];
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
var timeout = 120;

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
	exports.groups = groups;
	exports.Resource = Resource;
	exports.roles = roles;
	exports.users = users;
	exports.permissions = permissions;
  exports.initialize = initialize;
}

// END of rhqapi.js

