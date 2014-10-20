/*! redhat_access_angular_ui - v0.9.28 - 2014-10-20
 * Copyright (c) 2014 ;
 * Licensed 
 */
angular.module('gettext').run(['gettextCatalog', function (gettextCatalog) {
/* jshint -W100 */
/* jshint +W100 */
}]);
'use strict';
angular.module('RedhatAccess.cases', [
    'ui.router',
    'ui.bootstrap',
    'localytics.directives',
    'ngTable',
    'RedhatAccess.template',
    'RedhatAccess.security',
    'RedhatAccess.search',
    'RedhatAccess.ui-utils',
    'RedhatAccess.common',
    'RedhatAccess.header'
]).constant('CASE_EVENTS', {
    received: 'case-received'
}).constant('CHAT_SUPPORT', {
    enableChat: true,
    chatButtonToken: '573A0000000GmiP',
    chatLiveAgentUrlPrefix: 'https://d.la1w1.salesforceliveagent.com/chat',
    chatInitHashOne: '572A0000000GmiP',
    chatInitHashTwo: '00DA0000000HxWH',
    chatIframeHackUrlPrefix: 'https://rogsstest.force.com/chatHidden'
}).constant('STATUS', {
    open: 'open',
    closed: 'closed',
    both: 'both'
}).value('NEW_DEFAULTS', {
    'product': '',
    'version': ''
}).value('GLOBAL_CASE_CONFIG', {
    'showRecommendations': true,
    'showAttachments': true
}).value('NEW_CASE_CONFIG', {
    'showRecommendations': true,
    'showAttachments': true,
    'showServerSideAttachments': true,
    'productSortListFile': '/productSortList.txt',
    'isPCM': false
}).value('EDIT_CASE_CONFIG', {
    'showDetails': true,
    'showDescription': true,
    'showBugzillas': true,
    'showAttachments': true,
    'showRecommendations': true,
    'showComments': true,
    'showServerSideAttachments': true,
    'showEmailNotifications': true
}).config([
    '$stateProvider',
    function ($stateProvider) {
        $stateProvider.state('compact', {
            url: '/case/compact?sessionId',
            templateUrl: 'cases/views/compact.html'
        });
        $stateProvider.state('compact.edit', {
            url: '/{id:[0-9]{1,8}}',
            templateUrl: 'cases/views/compactEdit.html',
            controller: 'CompactEdit'
        });
        $stateProvider.state('edit', {
            url: '/case/{id:[0-9]{1,8}}?commentId',
            templateUrl: 'cases/views/edit.html',
            controller: 'Edit',
            reloadOnSearch: false
        });
        $stateProvider.state('new', {
            url: '/case/new',
            templateUrl: 'cases/views/new.html',
            controller: 'New'
        });
        $stateProvider.state('list', {
            url: '/case/list',
            templateUrl: 'cases/views/list.html',
            controller: 'List'
        });
        $stateProvider.state('searchCases', {
            url: '/case/search',
            templateUrl: 'cases/views/search.html',
            controller: 'Search'
        });
        $stateProvider.state('group', {
            url: '/case/group',
            controller: 'Group',
            templateUrl: 'cases/views/group.html'
        });
        $stateProvider.state('defaultGroup', {
            url: '/case/group/default',
            controller: 'DefaultGroup',
            templateUrl: 'cases/views/defaultGroup.html'
        });
        $stateProvider.state('editGroup', {
            url: '/case/group/{groupNumber}',
            controller: 'EditGroup',
            templateUrl: 'cases/views/editGroup.html'
        });
    }
]);
/*global angular */
'use strict';
/*global $ */
angular.module('RedhatAccess.common', [
	'RedhatAccess.ui-utils',
	'jmdobry.angular-cache'
]).config(["$angularCacheFactoryProvider", function($angularCacheFactoryProvider) {

}]).constant('RESOURCE_TYPES', {
	article: 'Article',
	solution: 'Solution'
}).factory('configurationService', [
	'$q',
	function($q) {
		var defer = $q.defer();
		var service = {
			setConfig: function(config) {
				defer.resolve(config);
			},
			getConfig: function() {
				return defer.promise;
			}
		};
		return service;
	}
]);
'use strict';
/*global $ */
angular.module('RedhatAccess.header', []).value('TITLE_VIEW_CONFIG', {
    show: 'false',
    titlePrefix: 'Red Hat Access: ',
    searchTitle: 'Search',
    caseListTitle: 'Support Cases',
    caseViewTitle: 'View/Modify Case',
    newCaseTitle: 'New Support Case',
    searchCaseTitle: 'Search Support Cases',
    logViewerTitle: 'Log',
    manageGroupsTitle: 'Manage Case Groups',
    editGroupTitle: 'Edit Case Group',
    defaultGroup: 'Manage Default Case Groups'
}).controller('TitleViewCtrl', [
    'TITLE_VIEW_CONFIG',
    '$scope',
    function (TITLE_VIEW_CONFIG, $scope) {
        $scope.showTitle = TITLE_VIEW_CONFIG.show;
        $scope.titlePrefix = TITLE_VIEW_CONFIG.titlePrefix;
        $scope.getPageTitle = function () {
            switch ($scope.page) {
            case 'search':
                return TITLE_VIEW_CONFIG.searchTitle;
            case 'caseList':
                return TITLE_VIEW_CONFIG.caseListTitle;
            case 'caseView':
                return TITLE_VIEW_CONFIG.caseViewTitle;
            case 'newCase':
                return TITLE_VIEW_CONFIG.newCaseTitle;
            case 'logViewer':
                return TITLE_VIEW_CONFIG.logViewerTitle;
            case 'searchCase':
                return TITLE_VIEW_CONFIG.searchCaseTitle;
            case 'manageGroups':
                return TITLE_VIEW_CONFIG.manageGroupsTitle;
            case 'editGroup':
                return TITLE_VIEW_CONFIG.editGroupTitle;
            default:
                return '';
            }
        };
    }
]).directive('rhaTitletemplate', function () {
    return {
        restrict: 'AE',
        scope: { page: '@' },
        templateUrl: 'common/views/title.html',
        controller: 'TitleViewCtrl'
    };
}).service('AlertService', [
    '$filter',
    'AUTH_EVENTS',
    '$rootScope',
    'RHAUtils',
    function ($filter, AUTH_EVENTS, $rootScope, RHAUtils) {
        var ALERT_TYPES = {
                DANGER: 'danger',
                SUCCESS: 'success',
                WARNING: 'warning'
            };
        this.alerts = [];
        //array of {message: 'some alert', type: '<type>'} objects
        this.clearAlerts = function () {
            this.alerts = [];
        };
        this.addAlert = function (alert) {
            this.alerts.push(alert);
        };
        this.removeAlert = function (alert) {
            this.alerts.splice(this.alerts.indexOf(alert), 1);
        };
        this.addDangerMessage = function (message) {
            return this.addMessage(message, ALERT_TYPES.DANGER);
        };
        this.addSuccessMessage = function (message) {
            return this.addMessage(message, ALERT_TYPES.SUCCESS);
        };
        this.addWarningMessage = function (message) {
            return this.addMessage(message, ALERT_TYPES.WARNING);
        };
        this.addMessage = function (message, type) {
            var alert = {
                    message: message,
                    type: type === null ? 'warning' : type
                };
            this.addAlert(alert);
            $('body,html').animate({ scrollTop: $('body').offset().top }, 100);
            //Angular adds a unique hash to each alert during data binding,
            //so the returned alert will be unique even if the
            //message and type are identical.
            return alert;
        };
        this.getErrors = function () {
            var errors = $filter('filter')(this.alerts, { type: ALERT_TYPES.DANGER });
            if (errors === null) {
                errors = [];
            }
            return errors;
        };
        this.addStrataErrorMessage = function (error) {
            if (RHAUtils.isNotEmpty(error)) {
                var errorText=error.message;
                if (error.xhr && error.xhr.responseText){
                    errorText = errorText.concat(' Message: ' + error.xhr.responseText);
                }
                var existingMessage = $filter('filter')(this.alerts, {
                        type: ALERT_TYPES.DANGER,
                        message: errorText,
                    });
                if (existingMessage.length < 1) {
                    this.addDangerMessage(errorText);
                }
            }
        };
        $rootScope.$on(AUTH_EVENTS.logoutSuccess, angular.bind(this, function () {
            this.clearAlerts();
            this.addMessage('You have successfully logged out of the Red Hat Customer Portal.');
        }));
        $rootScope.$on(AUTH_EVENTS.loginSuccess, angular.bind(this, function () {
            this.clearAlerts();
        }));
    }
]).directive('rhaAlert', function () {
    return {
        templateUrl: 'common/views/alert.html',
        restrict: 'A',
        controller: 'AlertController'
    };
}).controller('AlertController', [
    '$scope',
    'AlertService',
    function ($scope, AlertService) {
        $scope.AlertService = AlertService;
        $scope.closeable = true;
        $scope.closeAlert = function (index) {
            AlertService.alerts.splice(index, 1);
        };
        $scope.dismissAlerts = function () {
            AlertService.clearAlerts();
        };
    }
]).directive('rhaHeader', function () {
    return {
        templateUrl: 'common/views/header.html',
        restrict: 'A',
        scope: { page: '@' },
        controller: 'HeaderController'
    };
}).controller('HeaderController', [
    '$scope',
    'AlertService',
    function ($scope, AlertService) {
        /**
       * For some reason the rhaAlert directive's controller is not binding to the view.
       * Hijacking rhaAlert's parent controller (HeaderController) works
       * until a real solution is found.
       */
        $scope.AlertService = AlertService;
        $scope.closeable = true;
        $scope.closeAlert = function (index) {
            AlertService.alerts.splice(index, 1);
        };
        $scope.dismissAlerts = function () {
            AlertService.clearAlerts();
        };
    }
]);

'use strict';
/*jshint unused:vars */
var app = angular.module('RedhatAccess.ui-utils', ['gettext']);
//this is an example controller to provide tree data
// app.controller('TreeViewSelectorCtrl', ['$scope', 'TreeViewSelectorData',
//     function($scope, TreeViewSelectorData) {
//         $scope.name = 'Attachments';
//         $scope.attachmentTree = [];
//         TreeViewSelectorData.getTree('attachments').then(
//             function(tree) {
//                 $scope.attachmentTree = tree;
//             },
//             function() {
//             });
//     }
// ]);
app.service('RHAUtils', function () {
    /**
     * Generic function to decide if a simple object should be considered nothing
     */
    this.isEmpty = function (object) {
        if (object === undefined || object === null || object === '' || object.length === 0 || object === {}) {
            return true;
        }
        return false;
    };
    this.isNotEmpty = function (object) {
        return !this.isEmpty(object);
    };
});
//Wrapper service for translations
app.service('translate', [
    'gettextCatalog',
    function (gettextCatalog) {
        return function (str) {
            return gettextCatalog.getString(str);
        };
    }
]);
app.directive('rhaChoicetree', function () {
    return {
        template: '<ul><div rha-choice ng-repeat="choice in tree"></div></ul>',
        replace: true,
        transclude: true,
        restrict: 'A',
        scope: {
            tree: '=ngModel',
            rhaDisabled: '='
        }
    };
});
app.directive('optionsDisabled', ["$parse", function($parse) {
    var disableOptions = function(scope, attr, element, data, fnDisableIfTrue) {
        // refresh the disabled options in the select element.
        $('option[value!="?"]', element).each(function(i, e) {
            var locals = {};
            locals[attr] = data[i];
            $(this).attr('disabled', fnDisableIfTrue(scope, locals));
        });
    };
    return {
        priority: 0,
        link: function(scope, element, attrs, ctrl) {
            // parse expression and build array of disabled options
            var expElements = attrs.optionsDisabled.match(/^\s*(.+)\s+for\s+(.+)\s+in\s+(.+)?\s*/);
            var fnDisableIfTrue = $parse(expElements[1]);
            var options = expElements[3];
            scope.$watch(options, function(newValue, oldValue) {
                if(newValue) {
                    disableOptions(scope, expElements[2], element, newValue, fnDisableIfTrue);
                }
            }, true);
        }
    };
}]);
app.directive('rhaChoice', ["$compile", function ($compile) {
    return {
        restrict: 'A',
        templateUrl: 'common/views/treenode.html',
        link: function (scope, elm) {
            scope.choiceClicked = function (choice) {
                choice.checked = !choice.checked;
                function checkChildren(c) {
                    angular.forEach(c.children, function (c) {
                        c.checked = choice.checked;
                        checkChildren(c);
                    });
                }
                checkChildren(choice);
            };
            if (scope.choice.children.length > 0) {
                var childChoice = $compile('<div rha-choicetree ng-show="!choice.collapsed" ng-model="choice.children"></div>')(scope);
                elm.append(childChoice);
            }
        }
    };
}]);
app.factory('TreeViewSelectorData', [
    '$http',
    '$q',
    'TreeViewSelectorUtils',
    function ($http, $q, TreeViewSelectorUtils) {
        var service = {
                getTree: function (dataUrl, sessionId) {
                    var defer = $q.defer();
                    var tmpUrl = dataUrl;
                    if (sessionId) {
                        tmpUrl = tmpUrl + '?sessionId=' + encodeURIComponent(sessionId);
                    }
                    $http({
                        method: 'GET',
                        url: tmpUrl
                    }).success(function (data, status, headers, config) {
                        var tree = [];
                        TreeViewSelectorUtils.parseTreeList(tree, data);
                        defer.resolve(tree);
                    }).error(function (data, status, headers, config) {
                        defer.reject({});
                    });
                    return defer.promise;
                }
            };
        return service;
    }
]);
app.factory('TreeViewSelectorUtils', function () {
    var parseTreeNode = function (splitPath, tree, fullFilePath) {
        if (splitPath[0] !== undefined) {
            if (splitPath[0] !== '') {
                var node = splitPath[0];
                var match = false;
                var index = 0;
                for (var i = 0; i < tree.length; i++) {
                    if (tree[i].name === node) {
                        match = true;
                        index = i;
                        break;
                    }
                }
                if (!match) {
                    var nodeObj = {};
                    nodeObj.checked = isLeafChecked(node);
                    nodeObj.name = removeParams(node);
                    if (splitPath.length === 1) {
                        nodeObj.fullPath = removeParams(fullFilePath);
                    }
                    nodeObj.children = [];
                    tree.push(nodeObj);
                    index = tree.length - 1;
                }
                splitPath.shift();
                parseTreeNode(splitPath, tree[index].children, fullFilePath);
            } else {
                splitPath.shift();
                parseTreeNode(splitPath, tree, fullFilePath);
            }
        }
    };
    var removeParams = function (path) {
        if (path) {
            var split = path.split('?');
            return split[0];
        }
        return path;
    };
    var isLeafChecked = function (path) {
        if (path) {
            var split = path.split('?');
            if (split[1]) {
                var params = split[1].split('&');
                for (var i = 0; i < params.length; i++) {
                    if (params[i].indexOf('checked=true') !== -1) {
                        return true;
                    }
                }
            }
        }
        return false;
    };
    var hasSelectedLeaves = function (tree) {
        for (var i = 0; i < tree.length; i++) {
            if (tree[i] !== undefined) {
                if (tree[i].children.length === 0) {
                    //we only check leaf nodes
                    if (tree[i].checked === true) {
                        return true;
                    }
                } else {
                    if (hasSelectedLeaves(tree[i].children)) {
                        return true;
                    }
                }
            }
        }
        return false;
    };
    var getSelectedNames = function (tree, container) {
        for (var i = 0; i < tree.length; i++) {
            if (tree[i] !== undefined) {
                if (tree[i].children.length === 0) {
                    if (tree[i].checked === true) {
                        container.push(tree[i].fullPath);
                    }
                } else {
                    getSelectedNames(tree[i].children, container);
                }
            }
        }
    };
    var service = {
            parseTreeList: function (tree, data) {
                var files = data.split('\n');
                for (var i = 0; i < files.length; i++) {
                    var file = files[i];
                    var splitPath = file.split('/');
                    parseTreeNode(splitPath, tree, file);
                }
            },
            hasSelections: function (tree) {
                return hasSelectedLeaves(tree);
            },
            getSelectedLeaves: function (tree) {
                if (tree === undefined) {
                    return [];
                }
                var container = [];
                getSelectedNames(tree, container);
                return container;
            }
        };
    return service;
});
app.directive('rhaResizable', [
    '$window',
    '$timeout',
    function ($window) {
        var link = function (scope, element, attrs) {
            scope.onResizeFunction = function () {
                var distanceToTop = element[0].getBoundingClientRect().top;
                var height = $window.innerHeight - distanceToTop;
                element.css('height', height);
            };
            angular.element($window).bind('resize', function () {
                scope.onResizeFunction();    //scope.$apply();
            });
            angular.element($window).bind('click', function () {
                scope.onResizeFunction();    //scope.$apply();
            });
            if (attrs.rhaDomReady !== undefined) {
                scope.$watch('rhaDomReady', function (newValue) {
                    if (newValue) {
                        scope.onResizeFunction();
                    }
                });
            } else {
                scope.onResizeFunction();
            }
        };
        return {
            restrict: 'A',
            scope: { rhaDomReady: '=' },
            link: link
        };
    }
]);

//var testURL = 'http://localhost:8080/LogCollector/';
// angular module
'use strict';
angular.module('RedhatAccess.logViewer', [
    'angularTreeview',
    'ui.bootstrap',
    'RedhatAccess.search',
    'RedhatAccess.header'
]).config([
    '$stateProvider',
    function ($stateProvider) {
        $stateProvider.state('logviewer', {
            url: '/logviewer',
            templateUrl: 'log_viewer/views/log_viewer.html'
        });
    }
]).constant('LOGVIEWER_EVENTS', { allTabsClosed: 'allTabsClosed' }).value('hideMachinesDropdown', { value: false });
function returnNode(splitPath, tree, fullFilePath) {
    if (splitPath[0] !== undefined) {
        if (splitPath[0] !== '') {
            var node = splitPath[0];
            var match = false;
            var index = 0;
            for (var i in tree) {
                if (tree[i].roleName === node) {
                    match = true;
                    index = i;
                    break;
                }
            }
            if (!match) {
                var object = {};
                object.roleName = node;
                object.roleId = node;
                if (splitPath.length === 1) {
                    object.fullPath = fullFilePath;
                }
                object.children = [];
                tree.push(object);
                index = tree.length - 1;
            }
            splitPath.shift();
            returnNode(splitPath, tree[index].children, fullFilePath);
        } else {
            splitPath.shift();
            returnNode(splitPath, tree, fullFilePath);
        }
    }
}
function parseList(tree, data) {
    var files = data.split('\n');
    for (var i in files) {
        var file = files[i];
        var splitPath = file.split('/');
        returnNode(splitPath, tree, file);
    }
}

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search', [
    'ui.router',
    'RedhatAccess.template',
    'RedhatAccess.security',
    'ui.bootstrap',
    'ngSanitize',
    'RedhatAccess.ui-utils',
    'RedhatAccess.common',
    'RedhatAccess.header'
]).constant('SEARCH_PARAMS', { limit: 10 }).value('SEARCH_CONFIG', {
    openCaseRef: '#/case/new',
    showOpenCaseBtn: true
}).config([
    '$stateProvider',
    function ($stateProvider) {
        $stateProvider.state('search', {
            url: '/search',
            controller: 'SearchController',
            templateUrl: 'search/views/search.html'
        }).state('search_accordion', {
            url: '/search2',
            controller: 'SearchController',
            templateUrl: 'search/views/accordion_search.html'
        });
    }
]);
'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.security', [
    'ui.bootstrap',
    'RedhatAccess.template',
    'ui.router',
    'RedhatAccess.common',
    'RedhatAccess.header'
]).constant('AUTH_EVENTS', {
    loginSuccess: 'auth-login-success',
    loginFailed: 'auth-login-failed',
    logoutSuccess: 'auth-logout-success',
    sessionTimeout: 'auth-session-timeout',
    notAuthenticated: 'auth-not-authenticated',
    notAuthorized: 'auth-not-authorized',
    sessionIdChanged: 'sid-changed'
}).value('LOGIN_VIEW_CONFIG', { verbose: true }).value('SECURITY_CONFIG', {
    displayLoginStatus: true,
    autoCheckLogin: true,
    loginURL: '',
    logoutURL: '',
    forceLogin: false
});
'use strict';
/*global navigator, strata, angular*/
/*jshint camelcase: false */
/*jshint bitwise: false */
/*jshint unused:vars */
angular.module('RedhatAccess.common').factory('strataService', [
    '$q',
    'translate',
    'RHAUtils',
    '$angularCacheFactory',
    'RESOURCE_TYPES',
    function ($q, translate, RHAUtils, $angularCacheFactory, RESOURCE_TYPES) {
        $angularCacheFactory('strataCache', {
            capacity: 1000,
            maxAge: 900000,
            deleteOnExpire: 'aggressive',
            recycleFreq: 60000,
            cacheFlushInterval: 3600000,
            storageMode: 'sessionStorage',
            verifyIntegrity: true
        });
        var ie8 = false;
        if (navigator.appVersion.indexOf('MSIE 8.') !== -1) {
            ie8 = true;
        }
        var strataCache;
        if (!ie8) {
            strataCache = $angularCacheFactory.get('strataCache');
            $(window).unload(function () {
                strataCache.destroy();
            });
        }
        var errorHandler = function (message, xhr, response, status) {
            var translatedMsg = message;
            switch (status) {
            case 'Unauthorized':
                translatedMsg = translate('Unauthorized.');
                break; // case n:
                //   code block
                //   break;
            }
            this.reject({
                message: translatedMsg,
                xhr: xhr,
                response: response,
                status: status
            });
        };
        var service = {
            authentication: {
                checkLogin: function () {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('auth')) {
                        strata.addAccountNumber(strataCache.get('auth').number);
                        deferred.resolve(strataCache.get('auth'));
                    } else {
                        strata.checkLogin(function (result, authedUser) {
                            if (result) {
                                service.accounts.list().then(function (accountNumber) {
                                    service.accounts.get(accountNumber).then(function (account) {
                                        authedUser.account = account;
                                        strata.addAccountNumber(account.number);
                                        if (!ie8) {
                                            strataCache.put('auth', authedUser);
                                        }
                                        deferred.resolve(authedUser);
                                    });
                                }, function (error) {
                                    //TODO revisit this behavior
                                    authedUser.account = undefined;
                                    deferred.resolve(authedUser);
                                });
                            } else {
                                var error = {message: 'Unauthorized.'};
                                deferred.reject(error);
                            }
                        });
                    }
                    return deferred.promise;
                },
                setCredentials: function (username, password) {
                    return strata.setCredentials(username, password);
                },
                logout: function () {
                    if (!ie8) {
                        strataCache.removeAll();
                    }
                    strata.clearCredentials();
                }
            },
            entitlements: {
                get: function (showAll, ssoUserName) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('entitlements' + ssoUserName)) {
                        deferred.resolve(strataCache.get('entitlements' + ssoUserName));
                    } else {
                        strata.entitlements.get(showAll, function (entitlements) {
                            if (!ie8) {
                                strataCache.put('entitlements' + ssoUserName, entitlements);
                            }
                            deferred.resolve(entitlements);
                        }, angular.bind(deferred, errorHandler), ssoUserName);
                    }
                    return deferred.promise;
                }
            },
            problems: function (data, max) {
                var deferred = $q.defer();
                strata.problems(data, function (solutions) {
                    deferred.resolve(solutions);
                }, angular.bind(deferred, errorHandler), max);
                return deferred.promise;
            },
            recommendations: function (data, max) {
                var deferred = $q.defer();
                strata.recommendations(data, function (recommendations) {
                    deferred.resolve(recommendations);
                }, angular.bind(deferred, errorHandler), max);
                return deferred.promise;
            },
            solutions: {
                get: function (uri) {
                    var deferred = $q.defer();
                    var splitUri = uri.split('/');
                    uri = splitUri[splitUri.length - 1];
                    if (!ie8 && strataCache.get('solution' + uri)) {
                        deferred.resolve(strataCache.get('solution' + uri));
                    } else {
                        strata.solutions.get(uri, function (solution) {
                            solution.resource_type = RESOURCE_TYPES.solution; //Needed upstream
                            if (!ie8) {
                                strataCache.put('solution' + uri, solution);
                            }
                            deferred.resolve(solution);
                        }, function () {
                            //workaround for 502 from strata
                            //If the deferred is rejected then the parent $q.all()
                            //based deferred will fail. Since we don't need every
                            //recommendation just send back undefined
                            //and the caller can ignore the missing solution details.
                            deferred.resolve();
                        });
                    }
                    return deferred.promise;
                }
            },
            search: function (searchString, max) {
                var resultsDeferred = $q.defer();
                var deferreds = [];
                strata.search(
                    searchString,
                    function (entries) {
                        //retrieve details for each solution
                        if (entries !== undefined) {
                            entries.forEach(function (entry) {
                                var deferred = $q.defer();
                                deferreds.push(deferred.promise);
                                var cacheMiss = true;
                                if (entry.resource_type === RESOURCE_TYPES.solution) {
                                    if (!ie8 && strataCache.get('solution' + entry.uri)) {
                                        deferred.resolve(strataCache.get('solution' + entry.uri));
                                        cacheMiss = false;
                                    }

                                }
                                // else if (entry.resource_type === RESOURCE_TYPES.article) {
                                //     if (strataCache.get('article' + entry.uri)) {
                                //         deferred.resolve(strataCache.get('article' + entry.uri));
                                //         cacheMiss = false;
                                //     }
                                // }
                                if (cacheMiss) {
                                    strata.utils.getURI(entry.uri, entry.resource_type, function (type, info) {
                                        if (info !== undefined) {
                                            info.resource_type = type;
                                            if (!ie8 && (type === RESOURCE_TYPES.solution)) {
                                                strataCache.put('solution' + entry.uri, info);
                                            }
                                        }
                                        deferred.resolve(info);
                                    }, function (error) {
                                        deferred.resolve();
                                    });
                                }
                            });
                        }
                        $q.all(deferreds).then(
                            function (results) {
                                results.forEach(function (result) {
                                    if (result !== undefined) {
                                        results.push(result);
                                    }
                                });
                                resultsDeferred.resolve(results);
                            },
                            angular.bind(resultsDeferred, errorHandler));
                    },
                    angular.bind(resultsDeferred, errorHandler),
                    max,
                    false);
                return resultsDeferred.promise;
            },
            products: {
                list: function (ssoUserName) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('products' + ssoUserName)) {
                        deferred.resolve(strataCache.get('products' + ssoUserName));
                    } else {
                        strata.products.list(function (response) {
                            if (!ie8) {
                                strataCache.put('products' + ssoUserName, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler), ssoUserName);
                    }
                    return deferred.promise;
                },
                versions: function (productCode) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('versions-' + productCode)) {
                        deferred.resolve(strataCache.get('versions-' + productCode));
                    } else {
                        strata.products.versions(productCode, function (response) {
                            if (!ie8) {
                                strataCache.put('versions-' + productCode, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                    }
                    return deferred.promise;
                },
                get: function (productCode) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('product' + productCode)) {
                        deferred.resolve(strataCache.get('product' + productCode));
                    } else {
                        strata.products.get(productCode, function (response) {
                            if (!ie8) {
                                strataCache.put('product' + productCode, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                    }
                    return deferred.promise;
                }
            },
            groups: {
                get: function (groupNum, ssoUserName) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('groups' + ssoUserName)) {
                        deferred.resolve(strataCache.get('groups' + ssoUserName));
                    } else {
                        strata.groups.get(groupNum, function (response) {
                            if (!ie8) {
                                strataCache.put('groups' + ssoUserName, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler), ssoUserName);
                    }
                    return deferred.promise;
                },
                list: function (ssoUserName) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('groups' + ssoUserName)) {
                        deferred.resolve(strataCache.get('groups' + ssoUserName));
                    } else {
                        strata.groups.list(function (response) {
                            if (!ie8) {
                                strataCache.put('groups' + ssoUserName, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler), ssoUserName);
                    }
                    return deferred.promise;
                },
                remove: function (groupNum) {
                    var deferred = $q.defer();
                    strata.groups.remove(groupNum, function (response) {
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                },
                create: function (groupName) {
                    var deferred = $q.defer();
                    strata.groups.create(groupName, function (response) {
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                },
                update: function(groupName, groupnum){
                    var deferred = $q.defer();
                    strata.groups.update(groupName, groupnum, function (response) {
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                },
                createDefault: function(group){
                    var deferred = $q.defer();
                    strata.groups.createDefault(group, function (response) {
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                }
            },
            groupUsers: {
                update: function(users, accountId, groupnum){
                    var deferred = $q.defer();
                    strata.groupUsers.update(users, accountId, groupnum, function (response) {
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                }
            },
            accounts: {
                get: function (accountNumber) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('account' + accountNumber)) {
                        deferred.resolve(strataCache.get('account' + accountNumber));
                    } else {
                        strata.accounts.get(accountNumber, function (response) {
                            if (!ie8) {
                                strataCache.put('account' + accountNumber, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                    }
                    return deferred.promise;
                },
                users: function (accountNumber, group) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('users' + accountNumber + group)) {
                        deferred.resolve(strataCache.get('users' + accountNumber + group));
                    } else {
                        strata.accounts.users(accountNumber, function (response) {
                            if (!ie8) {
                                strataCache.put('users' + accountNumber + group, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler), group);
                    }
                    return deferred.promise;
                },
                list: function () {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('account')) {
                        deferred.resolve(strataCache.get('account'));
                    } else {
                        strata.accounts.list(function (response) {
                            if (!ie8) {
                                strataCache.put('account', response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                    }
                    return deferred.promise;
                }
            },
            cases: {
                csv: function () {
                    var deferred = $q.defer();
                    strata.cases.csv(function (response) {
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                },
                attachments: {
                    list: function (id) {
                        var deferred = $q.defer();
                        if (!ie8 && strataCache.get('attachments' + id)) {
                            deferred.resolve(strataCache.get('attachments' + id));
                        } else {
                            strata.cases.attachments.list(id, function (response) {
                                if (!ie8) {
                                    strataCache.put('attachments' + id, response);
                                }
                                deferred.resolve(response);
                            }, angular.bind(deferred, errorHandler));
                        }
                        return deferred.promise;
                    },
                    post: function (attachment, caseNumber) {
                        var deferred = $q.defer();
                        strata.cases.attachments.post(attachment, caseNumber, function (response, code, xhr) {
                            if (!ie8) {
                                strataCache.remove('attachments' + caseNumber);
                            }
                            deferred.resolve(xhr.getResponseHeader('Location'));
                        }, angular.bind(deferred, errorHandler));
                        return deferred.promise;
                    },
                    remove: function (id, caseNumber) {
                        var deferred = $q.defer();
                        strata.cases.attachments.remove(id, caseNumber, function (response) {
                            if (!ie8) {
                                strataCache.remove('attachments' + caseNumber);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                        return deferred.promise;
                    }
                },
                comments: {
                    get: function (id) {
                        var deferred = $q.defer();
                        if (!ie8 && strataCache.get('comments' + id)) {
                            deferred.resolve(strataCache.get('comments' + id));
                        } else {
                            strata.cases.comments.get(id, function (response) {
                                if (!ie8) {
                                    strataCache.put('comments' + id, response);
                                }
                                deferred.resolve(response);
                            }, angular.bind(deferred, errorHandler));
                        }
                        return deferred.promise;
                    },
                    post: function (caseNumber, text, isPublic, isDraft) {
                        var deferred = $q.defer();
                        strata.cases.comments.post(caseNumber, {
                            'text': text,
                            'draft': isDraft === true ? 'true' : 'false',
                            'public': isPublic === true ? 'true' : 'false'
                        }, function (response) {
                            if (!ie8) {
                                strataCache.remove('comments' + caseNumber);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                        return deferred.promise;
                    },
                    put: function (caseNumber, text, isDraft, isPublic, comment_id) {
                        var deferred = $q.defer();
                        strata.cases.comments.update(caseNumber, {
                            'text': text,
                            'draft': isDraft === true ? 'true' : 'false',
                            'public': isPublic === true ? 'true' : 'false',
                            'caseNumber': caseNumber,
                            'id': comment_id
                        }, comment_id, function (response) {
                            if (!ie8) {
                                strataCache.remove('comments' + caseNumber);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                        return deferred.promise;
                    }
                },
                notified_users: {
                    add: function (caseNumber, ssoUserName) {
                        var deferred = $q.defer();
                        strata.cases.notified_users.add(caseNumber, ssoUserName, function (response) {
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                        return deferred.promise;
                    },
                    remove: function (caseNumber, ssoUserName) {
                        var deferred = $q.defer();
                        strata.cases.notified_users.remove(caseNumber, ssoUserName, function (response) {
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                        return deferred.promise;
                    }
                },
                get: function (id) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('case' + id)) {
                        deferred.resolve([
                            strataCache.get('case' + id),
                            true
                        ]);
                    } else {
                        strata.cases.get(id, function (response) {
                            if (!ie8) {
                                strataCache.put('case' + id, response);
                            }
                            deferred.resolve([
                                response,
                                false
                            ]);
                        }, angular.bind(deferred, errorHandler));
                    }
                    return deferred.promise;
                },
                filter: function (params) {
                    var deferred = $q.defer();
                    if (RHAUtils.isEmpty(params)) {
                        params = {};
                    }
                    if (RHAUtils.isEmpty(params.count)) {
                        params.count = 50;
                    }
                    if (!ie8 && strataCache.get('filter' + JSON.stringify(params))) {
                        deferred.resolve(strataCache.get('filter' + JSON.stringify(params)));
                    } else {
                        strata.cases.filter(params, function (response) {
                            if (!ie8) {
                                strataCache.put('filter' + JSON.stringify(params), response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler));
                    }
                    return deferred.promise;
                },
                post: function (caseJSON) {
                    var deferred = $q.defer();
                    strata.cases.post(caseJSON, function (caseNumber) {
                        //Remove any case filters that are cached
                        if (!ie8) {
                            for (var k in strataCache.keySet()) {
                                if (~k.indexOf('filter')) {
                                    strataCache.remove(k);
                                }
                            }
                        }
                        deferred.resolve(caseNumber);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                },
                put: function (caseNumber, caseJSON) {
                    var deferred = $q.defer();
                    strata.cases.put(caseNumber, caseJSON, function (response) {
                        if (!ie8) {
                            strataCache.remove('case' + caseNumber);
                        }
                        deferred.resolve(response);
                    }, angular.bind(deferred, errorHandler));
                    return deferred.promise;
                }
            },
            values: {
                cases: {
                    severity: function () {
                        var deferred = $q.defer();
                        if (!ie8 && strataCache.get('severities')) {
                            deferred.resolve(strataCache.get('severities'));
                        } else {
                            strata.values.cases.severity(function (response) {
                                if (!ie8) {
                                    strataCache.put('severities', response);
                                }
                                deferred.resolve(response);
                            }, angular.bind(deferred, errorHandler));
                        }
                        return deferred.promise;
                    },
                    status: function () {
                        var deferred = $q.defer();
                        if (!ie8 && strataCache.get('statuses')) {
                            deferred.resolve(strataCache.get('statuses'));
                        } else {
                            strata.values.cases.status(function (response) {
                                if (!ie8) {
                                    strataCache.put('statuses', response);
                                }
                                deferred.resolve(response);
                            }, angular.bind(deferred, errorHandler));
                        }
                        return deferred.promise;
                    },
                    types: function () {
                        var deferred = $q.defer();
                        if (!ie8 && strataCache.get('types')) {
                            deferred.resolve(strataCache.get('types'));
                        } else {
                            strata.values.cases.types(function (response) {
                                if (!ie8) {
                                    strataCache.put('types', response);
                                }
                                deferred.resolve(response);
                            }, angular.bind(deferred, errorHandler));
                        }
                        return deferred.promise;
                    }
                }
            },
            users: {
                get: function (userId) {
                    var deferred = $q.defer();
                    if (!ie8 && strataCache.get('userId' + userId)) {
                        deferred.resolve(strataCache.get('userId' + userId));
                    } else {
                        strata.users.get(function (response) {
                            if (!ie8) {
                                strataCache.put('userId' + userId, response);
                            }
                            deferred.resolve(response);
                        }, angular.bind(deferred, errorHandler), userId);
                    }
                    return deferred.promise;
                },
                chatSession: {
                    post: function(){
                        var deferred = $q.defer();
                        if (!ie8 && strataCache.get('chatSession')) {
                            deferred.resolve(strataCache.get('chatSession'));
                        } else {
                            strata.users.chatSession.get(function (response) {
                                if (!ie8) {
                                    strataCache.put('chatSession', response);
                                }
                                deferred.resolve(response);
                            }, angular.bind(deferred, errorHandler));
                        }
                        return deferred.promise;
                    }
                }
            }
        };
        return service;
    }
]);
'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.security').controller('SecurityController', [
    '$scope',
    '$rootScope',
    'securityService',
    'SECURITY_CONFIG',
    function ($scope, $rootScope, securityService, SECURITY_CONFIG) {
        $scope.securityService = securityService;
        if (SECURITY_CONFIG.autoCheckLogin) {
            securityService.validateLogin(SECURITY_CONFIG.forceLogin);
        }
        $scope.displayLoginStatus = function () {
            return SECURITY_CONFIG.displayLoginStatus;
        };
    }
]);

'use strict';
angular.module('RedhatAccess.security').directive('rhaLoginstatus', function () {
    return {
        restrict: 'AE',
        scope: false,
        templateUrl: 'security/views/login_status.html'
    };
});
'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.security').factory('securityService', [
    '$rootScope',
    '$modal',
    'AUTH_EVENTS',
    '$q',
    'LOGIN_VIEW_CONFIG',
    'SECURITY_CONFIG',
    'strataService',
    'AlertService',
    'RHAUtils',
    function($rootScope, $modal, AUTH_EVENTS, $q, LOGIN_VIEW_CONFIG, SECURITY_CONFIG, strataService, AlertService, RHAUtils) {
        var service = {
            loginStatus: {
                isLoggedIn: false,
                verifying: false,
                userAllowedToManageCases: false,
                authedUser: {}
            },
            loginURL: SECURITY_CONFIG.loginURL,
            logoutURL: SECURITY_CONFIG.logoutURL,
            setLoginStatus: function(isLoggedIn, verifying, authedUser) {
                service.loginStatus.isLoggedIn = isLoggedIn;
                service.loginStatus.verifying = verifying;
                service.loginStatus.authedUser = authedUser;
                service.userAllowedToManageCases();
            },
            clearLoginStatus: function() {
                service.loginStatus.isLoggedIn = false;
                service.loginStatus.verifying = false;
                service.loginStatus.userAllowedToManageCases = false;
                service.loginStatus.authedUser = {};
            },
            setAccount: function(accountJSON) {
                service.loginStatus.account = accountJSON;
            },
            modalDefaults: {
                backdrop: 'static',
                keyboard: true,
                modalFade: true,
                templateUrl: 'security/views/login_form.html',
                windowClass: 'rha-login-modal'
            },
            modalOptions: {
                closeButtonText: 'Close',
                actionButtonText: 'OK',
                headerText: 'Proceed?',
                bodyText: 'Perform this action?',
                backdrop: 'static'
            },
            userAllowedToManageCases: function() {
                var canManage = false;
                if(service.loginStatus.authedUser.rights !== undefined){
                    for(var i = 0; i < service.loginStatus.authedUser.rights.right.length; i++){
                        if(service.loginStatus.authedUser.rights.right[i].name === 'portal_manage_cases' && service.loginStatus.authedUser.rights.right[i].has_access === true){
                            canManage = true;
                            break;
                        }
                    }
                }
                service.loginStatus.userAllowedToManageCases = canManage;
            },
            userAllowedToManageEmailNotifications: function(user) {
                if (RHAUtils.isNotEmpty(service.loginStatus.authedUser.account) && RHAUtils.isNotEmpty(service.loginStatus.authedUser.account) && service.loginStatus.authedUser.org_admin) {
                    return true;
                } else {
                    return false;
                }
            },
            userAllowedToManageGroups: function(user) {
                if (RHAUtils.isNotEmpty(service.loginStatus.authedUser.account) && RHAUtils.isNotEmpty(service.loginStatus.authedUser.account) && (!service.loginStatus.authedUser.account.has_group_acls || service.loginStatus.authedUser.account.has_group_acls && service.loginStatus.authedUser.org_admin)) {
                    return true;
                } else {
                    return false;
                }
            },
            getBasicAuthToken: function() {
                var defer = $q.defer();
                var token = localStorage.getItem('rhAuthToken');
                if (token !== undefined && token !== '') {
                    defer.resolve(token);
                    return defer.promise;
                } else {
                    service.login().then(function(authedUser) {
                        defer.resolve(localStorage.getItem('rhAuthToken'));
                    }, function(error) {
                        defer.resolve(error);
                    });
                    return defer.promise;
                }
            },
            loggingIn: false,
            initLoginStatus: function() {
                service.loggingIn = true;
                var defer = $q.defer();
                var wasLoggedIn = service.loginStatus.isLoggedIn;
                service.loginStatus.verifying = true;
                strataService.authentication.checkLogin().then(angular.bind(this, function(authedUser) {
                    service.setAccount(authedUser.account);
                    service.setLoginStatus(true, false, authedUser);
                    service.loggingIn = false;
                    //We don't want to resend the AUTH_EVENTS.loginSuccess if we are already logged in
                    if (wasLoggedIn === false) {
                        $rootScope.$broadcast(AUTH_EVENTS.loginSuccess);
                    }
                    defer.resolve(authedUser.name);
                }), angular.bind(this, function(error) {
                    service.clearLoginStatus();
                    AlertService.addStrataErrorMessage(error);
                    service.loggingIn = false;
                    defer.reject(error);
                }));
                return defer.promise;
            },
            validateLogin: function(forceLogin) {
                var defer = $q.defer();
                //var that = this;
                if (!forceLogin) {
                    service.initLoginStatus().then(function(username) {
                        defer.resolve(username);
                    }, function(error) {
                        defer.reject(error);
                    });
                    return defer.promise;
                } else {
                    service.initLoginStatus().then(function(username) {
                        defer.resolve(username);
                    }, function(error) {
                        service.login().then(function(authedUser) {
                            defer.resolve(authedUser.name);
                        }, function(error) {
                            defer.reject(error);
                        });
                    });
                    return defer.promise;
                }
            },
            login: function() {
                return service.showLogin(service.modalDefaults, service.modalOptions);
            },
            logout: function() {
                strataService.authentication.logout();
                service.clearLoginStatus();
                $rootScope.$broadcast(AUTH_EVENTS.logoutSuccess);
            },
            showLogin: function(customModalDefaults, customModalOptions) {
                //var that = this;
                //Create temp objects to work with since we're in a singleton service
                var tempModalDefaults = {};
                var tempModalOptions = {};
                //Map angular-ui modal custom defaults to modal defaults defined in service
                angular.extend(tempModalDefaults, service.modalDefaults, customModalDefaults);
                //Map modal.html $scope custom properties to defaults defined in service
                angular.extend(tempModalOptions, service.modalOptions, customModalOptions);
                if (!tempModalDefaults.controller) {
                    tempModalDefaults.controller = [
                        '$scope',
                        '$modalInstance',
                        function($scope, $modalInstance) {
                            $scope.user = {
                                user: null,
                                password: null
                            };
                            $scope.status = {
                                authenticating: false
                            };
                            $scope.useVerboseLoginView = LOGIN_VIEW_CONFIG.verbose;
                            $scope.modalOptions = tempModalOptions;
                            $scope.modalOptions.ok = function(result) {
                                //Hack below is needed to handle autofill issues
                                //@see https://github.com/angular/angular.js/issues/1460
                                //BEGIN HACK
                                $scope.status.authenticating = true;
                                $scope.user.user = $('#rha-login-user-id').val();
                                $scope.user.password = $('#rha-login-password').val();
                                //END HACK
                                var resp = strataService.authentication.setCredentials($scope.user.user, $scope.user.password);
                                if (resp) {
                                    service.initLoginStatus().then(
                                        function(authedUser) {
                                            $scope.user.password = '';
                                            $scope.authError = null;
                                            try {
                                                $modalInstance.close(authedUser);
                                            } catch (err) {}
                                            $scope.status.authenticating = false;
                                        },
                                        function(error) {
                                            if ($scope.$root.$$phase !== '$apply' && $scope.$root.$$phase !== '$digest') {
                                                $scope.$apply(function() {
                                                    $scope.authError = 'Login Failed!';
                                                });
                                            } else {
                                                $scope.authError = 'Login Failed!';
                                            }
                                            $scope.status.authenticating = false;
                                        }
                                    );
                                }else {
                                    $scope.authError = 'Login Failed!';
                                    $scope.status.authenticating = false;
                                }
                            };
                            $scope.modalOptions.close = function() {
                                $scope.status.authenticating = false;
                                $modalInstance.dismiss('User Canceled Login');
                            };
                        }
                    ];
                }
                return $modal.open(tempModalDefaults).result;
            }
        };
        return service;
    }
]);

'use strict';
/*jshint unused:vars, camelcase:false */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').controller('SearchController', [
    '$scope',
    '$location',
    'SearchResultsService',
    'SEARCH_CONFIG',
    'securityService',
    'AlertService',
    function ($scope, $location, SearchResultsService, SEARCH_CONFIG, securityService, AlertService) {
        $scope.SearchResultsService = SearchResultsService;
        $scope.results = SearchResultsService.results;
        $scope.selectedSolution = SearchResultsService.currentSelection;
        $scope.searchInProgress = SearchResultsService.searchInProgress;
        $scope.currentSearchData = SearchResultsService.currentSearchData;
        $scope.itemsPerPage = 3;
        $scope.maxPagerSize = 5;
        $scope.selectPage = function (pageNum) {
            var start = $scope.itemsPerPage * (pageNum - 1);
            var end = start + $scope.itemsPerPage;
            end = end > SearchResultsService.results.length ? SearchResultsService.results.length : end;
            $scope.results = SearchResultsService.results.slice(start, end);
        };
        $scope.getOpenCaseRef = function () {
            if (SEARCH_CONFIG.openCaseRef !== undefined) {
                //TODO data may be complex type - need to normalize to string in future
                return SEARCH_CONFIG.openCaseRef + '?data=' + SearchResultsService.currentSearchData.data;
            } else {
                return '#/case/new?data=' + SearchResultsService.currentSearchData.data;
            }
        };
        $scope.solutionSelected = function (index) {
            var response = $scope.results[index];
            SearchResultsService.setSelected(response, index);
        };
        $scope.search = function (searchStr, limit) {
            SearchResultsService.search(searchStr, limit);
        };
        $scope.diagnose = function (data, limit) {
            SearchResultsService.diagnose(data, limit);
        };
        $scope.triggerAnalytics = function ($event) {
            if (this.isopen && window.chrometwo_require !== undefined && $location.path() === '/case/new') {
                chrometwo_require(['analytics/main'], function (analytics) {
                    analytics.trigger('OpenSupportCaseRecommendationClick', $event);
                });
            }
        };
        $scope.$watch(function () {
            return SearchResultsService.currentSelection;
        }, function (newVal) {
            $scope.selectedSolution = newVal;
        });
    }
]);

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').directive('rhaAccordionsearchresults', [
    'SEARCH_CONFIG',
    function (SEARCH_CONFIG) {
        return {
            restrict: 'AE',
            scope: false,
            templateUrl: 'search/views/accordion_search_results.html',
            link: function (scope, element, attr) {
                scope.showOpenCaseBtn = function () {
                    if (SEARCH_CONFIG.showOpenCaseBtn && (attr && attr.opencase === 'true')) {
                        return true;
                    } else {
                        return false;
                    }
                };
            }
        };
    }
]);

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').directive('rhaListsearchresults', function () {
    return {
        restrict: 'AE',
        scope: false,
        templateUrl: 'search/views/list_search_results.html'
    };
});

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').directive('rhaResultdetaildisplay', [
    'RESOURCE_TYPES',
    function (RESOURCE_TYPES) {
        return {
            restrict: 'AE',
            scope: { result: '=' },
            link: function (scope, element, attr) {
                scope.isSolution = function () {
                    if (scope.result !== undefined && scope.result.resource_type !== undefined) {
                        if (scope.result.resource_type === RESOURCE_TYPES.solution) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    return false;
                };
                scope.isArticle = function () {
                    if (scope.result !== undefined && scope.result.resource_type !== undefined) {
                        if (scope.result.resource_type === RESOURCE_TYPES.article) {
                            return true;
                        } else {
                            return false;
                        }
                    }
                    return false;
                };
                scope.getSolutionResolution = function () {
                    var resolutionHtml = '';
                    if (scope.result.resolution !== undefined) {
                        resolutionHtml = scope.result.resolution.html;
                    }
                    return resolutionHtml;
                };
                scope.getArticleHtml = function () {
                    if (scope.result === undefined) {
                        return '';
                    }
                    if (scope.result.body !== undefined) {
                        if (scope.result.body.html !== undefined) {
                            //this is for newer version of strata
                            return scope.result.body.html;
                        } else {
                            //handle old markdown format
                            return scope.result.body;
                        }
                    } else {
                        return '';
                    }
                };
            },
            templateUrl: 'search/views/resultDetail.html'
        };
    }
]);

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').directive('rhaSearchform', function () {
    return {
        restrict: 'AE',
        scope: false,
        templateUrl: 'search/views/search_form.html'
    };
});

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').directive('rhaStandardsearch', function () {
    return {
        restrict: 'AE',
        scope: false,
        templateUrl: 'search/views/standard_search.html'
    };
});

/*jshint camelcase: false */
'use strict';
/*jshint unused:vars */
/**
 * @ngdoc module
 * @name
 *
 * @description
 *
 */
angular.module('RedhatAccess.search').factory('SearchResultsService', [
    '$q',
    '$rootScope',
    'AUTH_EVENTS',
    'RESOURCE_TYPES',
    'SEARCH_PARAMS',
    'AlertService',
    'securityService',
    'strataService',
    function ($q, $rootScope, AUTH_EVENTS, RESOURCE_TYPES, SEARCH_PARAMS, AlertService, securityService, strataService) {
        var searchArticlesOrSolutions = function (searchString, limit) {
            //var that = this;
            if (limit === undefined || limit < 1) {
                limit = SEARCH_PARAMS.limit;
            }
            service.clear();
            AlertService.clearAlerts();
            service.setCurrentSearchData(searchString, 'search');
            strataService.search(searchString, limit).then(
                function (results) {
                    if (results.length === 0) {
                        AlertService.addSuccessMessage('No solutions found.');
                    }
                    results.forEach(function (result) {
                        if (result !== undefined) {
                            service.add(result);
                        }
                    });
                    service.searchInProgress.value = false;
                }, function (error) {
                    service.searchInProgress.value = false;
                });
        };
        var searchProblems = function (data, limit) {
            if (limit === undefined || limit < 1) {
                limit = SEARCH_PARAMS.limit;
            }
            service.clear();
            AlertService.clearAlerts();
            var deferreds = [];
            service.searchInProgress.value = true;
            service.setCurrentSearchData(data, 'diagnose');
            strataService.problems(data, limit).then(
                function (solutions) {
                    //retrieve details for each solution
                    if (solutions !== undefined) {
                        if (solutions.length === 0) {
                            AlertService.addSuccessMessage('No solutions found.');
                        }
                        solutions.forEach(function (solution) {
                            var deferred = $q.defer();
                            deferreds.push(deferred.promise);
                            strataService.solutions.get(solution.uri).then(
                                function (solution) {
                                    deferred.resolve(solution);
                                },
                                function (error) {
                                    deferred.resolve();
                                });
                        });
                    } else {
                        AlertService.addSuccessMessage('No solutions found.');
                    }
                    $q.all(deferreds).then(function (solutions) {
                        solutions.forEach(function (solution) {
                            if (solution !== undefined) {
                                service.add(solution);
                            }
                        });
                        service.searchInProgress.value = false;
                    }, function (error) {
                        service.searchInProgress.value = false;
                    });
                },
                function (error) {
                    service.searchInProgress.value = false;
                    AlertService.addDangerMessage(error);
                });
        };
        var service = {
            results: [],
            currentSelection: {
                data: {},
                index: -1
            },
            searchInProgress: {
                value: false
            },
            currentSearchData: {
                data: '',
                method: ''
            },
            add: function (result) {
                this.results.push(result);
            },
            clear: function () {
                this.results.length = 0;
                this.setSelected({}, -1);
                this.setCurrentSearchData('', '');
            },
            setSelected: function (selection, index) {
                this.currentSelection.data = selection;
                this.currentSelection.index = index;
            },
            setCurrentSearchData: function (data, method) {
                this.currentSearchData.data = data;
                this.currentSearchData.method = method;
            },
            search: function (searchString, limit) {
                this.searchInProgress.value = true;
                var that = this;
                securityService.validateLogin(true).then(function (authedUser) {
                    searchArticlesOrSolutions(searchString, limit);
                }, function (error) {
                    that.searchInProgress.value = false;
                    AlertService.addDangerMessage('You must be logged in to use this functionality.');
                });
            },
            diagnose: function (data, limit) {
                this.searchInProgress.value = true;
                var that = this;
                securityService.validateLogin(true).then(function (authedUser) {
                    searchProblems(data, limit);
                }, function (error) {
                    that.searchInProgress.value = false;
                    AlertService.addDangerMessage('You must be logged in to use this functionality.');
                });
            }
        };
        $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            service.clear.apply(service);
        });
        return service;
    }
]);
'use strict';
/*global $ */
/*jshint expr: true, camelcase: false, newcap: false */
angular.module('RedhatAccess.cases').controller('EditGroup', [
    '$scope',
    '$rootScope',
    'strataService',
    'AlertService',
    '$filter',
    'ngTableParams',
    'GroupUserService',
    'SearchBoxService',
    '$location',
    'securityService',
    'RHAUtils',
    'AUTH_EVENTS',
    function ($scope, $rootScope, strataService, AlertService, $filter, ngTableParams, GroupUserService, SearchBoxService, $location, securityService, RHAUtils, AUTH_EVENTS) {
        $scope.GroupUserService = GroupUserService;
        $scope.listEmpty = false;
        $scope.selectedGroup = {};
        $scope.usersOnScreen = [];
        $scope.usersOnAccount = [];
        $scope.accountNumber = null;
        $scope.isUsersPrestine = true;
        $scope.isGroupPrestine = true;
        
        var reloadTable = false;
        var tableBuilt = false;
        var buildTable = function () {
            $scope.tableParams = new ngTableParams({
                page: 1,
                count: 10,
                sorting: { sso_username: 'asc' }
            }, {
                total: $scope.usersOnAccount.length,
                getData: function ($defer, params) {
                    var orderedData = $filter('filter')($scope.usersOnAccount, SearchBoxService.searchTerm);
                    orderedData = params.sorting() ? $filter('orderBy')(orderedData, params.orderBy()) : orderedData;
                    orderedData.length < 1 ? $scope.listEmpty = true : $scope.listEmpty = false;
                    var pageData = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                    $scope.tableParams.total(orderedData.length);
                    $scope.usersOnScreen = pageData;
                    $defer.resolve(pageData);
                }
            });
            $scope.tableParams.settings().$scope = $scope;
            GroupUserService.reloadTable = function () {
                $scope.tableParams.reload();
            };
            tableBuilt = true;
        };
        $scope.init = function() {
            if(securityService.userAllowedToManageGroups()){
                var loc = $location.url().split('/');
                $scope.accountNumber = securityService.loginStatus.authedUser.account_number;
                strataService.groups.get(loc[3]).then(function (group) {
                    $scope.selectedGroup = group;
                    strataService.accounts.users($scope.accountNumber, $scope.selectedGroup.number).then(function (users) {
                        $scope.usersOnAccount = users;
                        buildTable();
                        $scope.usersLoading = false;
                        if(reloadTable){
                            //GroupUserService.reloadTable();
                            reloadTable = false;
                        }
                    }, function (error) {
                        $scope.usersLoading = false;
                        AlertService.addStrataErrorMessage(error);
                    });
                }, function (error) {
                    $scope.usersLoading = false;
                    AlertService.addStrataErrorMessage(error);
                });
            }else{
                $scope.usersLoading = false;
                AlertService.addStrataErrorMessage('User does not have proper credentials to manage case groups.');
            }
        };
        $scope.saveGroup = function () {
            if(!$scope.isGroupPrestine){
                strataService.groups.update($scope.selectedGroup.name, $scope.selectedGroup.number).then(function (response) {
                    AlertService.addSuccessMessage('Case group successfully updated.');
                    $scope.isGroupPrestine = true;
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                });
            }
            if(!$scope.isUsersPrestine){
                strataService.groupUsers.update($scope.usersOnAccount, $scope.accountNumber, $scope.selectedGroup.number).then(function(response) {
                    $scope.isUsersPrestine = true;
                    AlertService.addSuccessMessage('Case users successfully updated.');
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                });
            }
        };

        $scope.onMasterReadCheckboxClicked = function (masterReadSelected) {
            for(var i = 0; i < $scope.usersOnAccount.length; i++){
                $scope.usersOnAccount[i].access = masterReadSelected;
            }
            $scope.isUsersPrestine = false;
        };
        
        $scope.onMasterWriteCheckboxClicked = function (masterWriteSelected) {
            for(var i = 0; i < $scope.usersOnAccount.length; i++){
                $scope.usersOnAccount[i].write = masterWriteSelected;
            }
            $scope.isUsersPrestine = false;
        };

        $scope.writeAccessToggle = function(user){
            if(user.write && !user.access){
                user.access = true;
            }
            $scope.isUsersPrestine = false;
        };

        $scope.cancel = function(){
            $location.path('/case/group');
        };

        $scope.toggleUsersPrestine = function(){
            $scope.isUsersPrestine = false;
        };

        $scope.toggleGroupPrestine = function(){
            $scope.isGroupPrestine = false;
        };

        $scope.usersLoading = true;
        if (securityService.loginStatus.isLoggedIn) {
            $scope.init();

        } else {
            $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
                $scope.init();
            });
        }

        $scope.authEventLogoutSuccess = $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            $scope.selectedGroup = {};
            $scope.usersOnScreen = [];
            $scope.usersOnAccount = [];
            $scope.accountNumber = null;
            reloadTable = true;
        });

        $scope.$on('$destroy', function () {
            $scope.authEventLogoutSuccess();
        });
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('AccountSelect', [
    '$scope',
    'strataService',
    'AlertService',
    'CaseService',
    'RHAUtils',
    function ($scope, strataService, AlertService, CaseService, RHAUtils) {
        $scope.CaseService = CaseService;
        $scope.selectUserAccount = function () {
            $scope.loadingAccountNumber = true;
            strataService.accounts.list().then(function (response) {
                $scope.loadingAccountNumber = false;
                CaseService.account.number = response;
                $scope.populateAccountSpecificFields();
            }, function (error) {
                $scope.loadingAccountNumber = false;
                AlertService.addStrataErrorMessage(error);
            });
        };
        $scope.alertInstance = null;
        $scope.populateAccountSpecificFields = function () {
            if (RHAUtils.isNotEmpty(CaseService.account.number)) {
                strataService.accounts.get(CaseService.account.number).then(function () {
                    if (RHAUtils.isNotEmpty($scope.alertInstance)) {
                        AlertService.removeAlert($scope.alertInstance);
                    }
                    CaseService.populateUsers();
                }, function () {
                    if (RHAUtils.isNotEmpty($scope.alertInstance)) {
                        AlertService.removeAlert($scope.alertInstance);
                    }
                    $scope.alertInstance = AlertService.addWarningMessage('Account not found.');
                    CaseService.users = [];
                });
            }
        };
    }
]);
'use strict';
/*global $, draftComment*/
/*jshint camelcase: false, expr: true*/
angular.module('RedhatAccess.cases').controller('AddCommentSection', [
    '$scope',
    'strataService',
    'CaseService',
    'AlertService',
    'securityService',
    '$timeout',
    'RHAUtils',
    function ($scope, strataService, CaseService, AlertService, securityService, $timeout, RHAUtils) {
        $scope.CaseService = CaseService;
        $scope.securityService = securityService;
        $scope.addingComment = false;
        $scope.progressCount = 0;
        $scope.maxCommentLength = '32000';

        $scope.addComment = function () {
            $scope.addingComment = true;
            if (!securityService.loginStatus.authedUser.is_internal) {
                CaseService.isCommentPublic = true;
            }
            var onSuccess = function (response) {
                if (RHAUtils.isNotEmpty($scope.saveDraftPromise)) {
                    $timeout.cancel($scope.saveDraftPromise);
                }
                CaseService.commentText = '';
                CaseService.disableAddComment = true;
                //TODO: find better way than hard code
                if (!securityService.loginStatus.authedUser.is_internal && CaseService.kase.status.name === 'Closed') {
                    var status = { name: 'Waiting on Red Hat' };
                    CaseService.kase.status = status;
                }

                if(securityService.loginStatus.authedUser.is_internal){
                    if (CaseService.kase.status.name === 'Waiting on Red Hat') {
                        var status = { name: 'Waiting on Customer' };
                        CaseService.kase.status = status;
                    }
                }else {
                    if (CaseService.kase.status.name === 'Waiting on Customer') {
                        var status = { name: 'Waiting on Red Hat' };
                        CaseService.kase.status = status;
                    }
                }
                

                CaseService.populateComments(CaseService.kase.case_number).then(function (comments) {
                    $scope.addingComment = false;
                    $scope.savingDraft = false;
                    $scope.draftSaved = false;
                    CaseService.draftComment = undefined;
                });
                $scope.progressCount = 0;

                if(securityService.loginStatus.authedUser.sso_username !== undefined && CaseService.updatedNotifiedUsers.indexOf(securityService.loginStatus.authedUser.sso_username) === -1){
                    strataService.cases.notified_users.add(CaseService.kase.case_number, securityService.loginStatus.authedUser.sso_username).then(function () {
                        CaseService.updatedNotifiedUsers.push(securityService.loginStatus.authedUser.sso_username);
                    }, function (error) {
                        AlertService.addStrataErrorMessage(error);
                    });
                }
                
            };
            var onError = function (error) {
                AlertService.addStrataErrorMessage(error);
                $scope.addingComment = false;
                $scope.progressCount = 0;
            };
            if (RHAUtils.isNotEmpty(CaseService.draftComment)) {
                strataService.cases.comments.put(CaseService.kase.case_number, CaseService.commentText, false, CaseService.isCommentPublic, CaseService.draftComment.id).then(onSuccess, onError);
            } else {
                strataService.cases.comments.post(CaseService.kase.case_number, CaseService.commentText, CaseService.isCommentPublic, false).then(onSuccess, onError);
            }
        };
        $scope.saveDraftPromise;
        $scope.onNewCommentKeypress = function () {
            if (RHAUtils.isNotEmpty(CaseService.commentText) && !$scope.addingComment) {
                CaseService.disableAddComment = false;
                $timeout.cancel($scope.saveDraftPromise);
                $scope.saveDraftPromise = $timeout(function () {
                    if (!$scope.addingComment && CaseService.commentText !== '') {
                        $scope.saveDraft();
                    }
                }, 5000);
            } else if (RHAUtils.isEmpty(CaseService.commentText)) {
                CaseService.disableAddComment = true;
            }
        };
        $scope.$watch('CaseService.commentText', function() {
            $scope.maxCharacterCheck();
        });
        $scope.maxCharacterCheck = function() {
            if (CaseService.commentText !== undefined && $scope.maxCommentLength  > CaseService.commentText.length) {
                var count = CaseService.commentText.length * 100 / $scope.maxCommentLength ;
                parseInt(count);
                $scope.progressCount = Math.round(count * 100) / 100;
            }
        };
        $scope.saveDraft = function () {
            $scope.savingDraft = true;
            if (!securityService.loginStatus.authedUser.is_internal) {
                CaseService.isCommentPublic = true;
            }
            var onSuccess = function (commentId) {
                $scope.savingDraft = false;
                $scope.draftSaved = true;
                CaseService.draftComment = {
                    'text': CaseService.commentText,
                    'id': RHAUtils.isNotEmpty(commentId) ? commentId : CaseService.draftComment.id,
                    'draft': true,
                    'public': CaseService.isCommentPublic,
                    'case_number': CaseService.kase.case_number
                };
            };
            var onFailure = function (error) {
                AlertService.addStrataErrorMessage(error);
                $scope.savingDraft = false;
            };
            if (RHAUtils.isNotEmpty(CaseService.draftComment)) {
                //draft update
                strataService.cases.comments.put(CaseService.kase.case_number, CaseService.commentText, true, CaseService.isCommentPublic, CaseService.draftComment.id).then(onSuccess, onFailure);
            } else {
                //initial draft save
                strataService.cases.comments.post(CaseService.kase.case_number, CaseService.commentText, CaseService.isCommentPublic, true).then(onSuccess, onFailure);
            }
        };
    }
]);

'use strict';
/*global $ */
angular.module('RedhatAccess.cases').controller('AttachLocalFile', [
    '$scope',
    '$sce',
    'RHAUtils',
    'AlertService',
    'AttachmentsService',
    'securityService',
    function ($scope, $sce, RHAUtils,AlertService, AttachmentsService, securityService) {
        $scope.AttachmentsService = AttachmentsService;
        $scope.NO_FILE_CHOSEN = 'No file chosen';
        $scope.fileDescription = '';
        var maxFileSize = 250000000;

        $scope.parseArtifactHtml = function () {
            var parsedHtml = '';
            if (RHAUtils.isNotEmpty(AttachmentsService.suggestedArtifact.description)) {
                var rawHtml = AttachmentsService.suggestedArtifact.description.toString();
                parsedHtml = $sce.trustAsHtml(rawHtml);
            }
            return parsedHtml;
        };
        $scope.clearSelectedFile = function () {
            $scope.fileName = $scope.NO_FILE_CHOSEN;
            $scope.fileDescription = '';
        };
        $scope.addFile = function () {
            /*jshint camelcase: false */
            var data = new FormData();
            data.append('file', $scope.fileObj);
            data.append('description', $scope.fileDescription);
            AttachmentsService.addNewAttachment({
                file_name: $scope.fileName,
                description: $scope.fileDescription,
                length: $scope.fileSize,
                created_by: securityService.loginStatus.authedUser.loggedInUser,
                created_date: new Date().getTime(),
                file: data
            });
            $scope.clearSelectedFile();
        };
        $scope.getFile = function () {
            $('#fileUploader').click();
        };
        $scope.selectFile = function (file) {
            if(file.size !== undefined){
                if(file.size < maxFileSize){
                    $scope.fileObj = file;
                    $scope.fileSize = $scope.fileObj.size;
                    $scope.fileName = $scope.fileObj.name;
                    $scope.$apply();
                } else {
                    AlertService.addDangerMessage(file.name + ' cannot be attached because it is larger the 250MB. Please FTP large files to dropbox.redhat.com.');
                }
                $('#fileUploader')[0].value = '';
            } else {
                $scope.fileName = file;
                $scope.$apply();
            }
        };

        $('#fileUploader').change(function(e){
            if(e.target.files !== undefined){
                $scope.selectFile(e.target.files[0]);
            } else{
                $scope.selectFile(e.target.value);
            }
        });
        $scope.clearSelectedFile();
    }
]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('AttachmentsSection', [
    '$scope',
    'AttachmentsService',
    'CaseService',
    'TreeViewSelectorUtils',
    'EDIT_CASE_CONFIG',
    function ($scope, AttachmentsService, CaseService, TreeViewSelectorUtils, EDIT_CASE_CONFIG) {
        $scope.rhaDisabled = !EDIT_CASE_CONFIG.showAttachments;
        $scope.showServerSideAttachments = EDIT_CASE_CONFIG.showServerSideAttachments;
        $scope.AttachmentsService = AttachmentsService;
        $scope.CaseService = CaseService;
        $scope.TreeViewSelectorUtils = TreeViewSelectorUtils;
        $scope.doUpdate = function () {
            $scope.updatingAttachments = true;
            AttachmentsService.updateAttachments(CaseService.kase.case_number).then(function () {
                $scope.updatingAttachments = false;
            }, function (error) {
                $scope.updatingAttachments = false;
            });
        };
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('BackEndAttachmentsCtrl', [
    '$scope',
    '$location',
    'TreeViewSelectorData',
    'AttachmentsService',
    'NEW_CASE_CONFIG',
    'EDIT_CASE_CONFIG',
    function ($scope, $location, TreeViewSelectorData, AttachmentsService, NEW_CASE_CONFIG, EDIT_CASE_CONFIG) {
        $scope.name = 'Attachments';
        $scope.attachmentTree = [];
        var newCase = false;
        var editCase = false;
        if ($location.path().indexOf('new') > -1) {
            newCase = true;
        } else {
            editCase = true;
        }
        if (!$scope.rhaDisabled && newCase && NEW_CASE_CONFIG.showServerSideAttachments || !$scope.rhaDisabled && editCase && EDIT_CASE_CONFIG.showServerSideAttachments) {
            var sessionId = $location.search().sessionId;
            TreeViewSelectorData.getTree('attachments', sessionId).then(function (tree) {
                $scope.attachmentTree = tree;
                AttachmentsService.updateBackEndAttachments(tree);
            }, function () {
            });
        }
    }
]);
'use strict';
/*jshint camelcase: false, expr: true*/
//Saleforce hack---
//we have to monitor stuff on the window object
//because the liveagent code generated by Salesforce is not
//designed for angularjs.
//We create fake buttons that we give to the salesforce api so we can track
//chat availability without having to write a complete rest client.
window.fakeOnlineButton = { style: { display: 'none' } };
window.fakeOfflineButton = { style: { display: 'none' } };
//
angular.module('RedhatAccess.cases').controller('ChatButton', [
    '$scope',
    'CaseService',
    'securityService',
    'strataService',
    'AlertService',
    'CHAT_SUPPORT',
    'AUTH_EVENTS',
    '$rootScope',
    '$sce',
    '$http',
    '$interval',
    function ($scope, CaseService, securityService, strataService, AlertService, CHAT_SUPPORT, AUTH_EVENTS, $rootScope, $sce, $http, $interval) {
        $scope.securityService = securityService;
        if (window.chatInitialized === undefined) {
            window.chatInitialized = false;
        }
        $scope.checkChatButtonStates = function () {
            $scope.chatAvailable = window.fakeOnlineButton.style.display !== 'none';
        };
        $scope.timer = null;
        $scope.chatHackUrl = $sce.trustAsResourceUrl(CHAT_SUPPORT.chatIframeHackUrlPrefix);
        $scope.setChatIframeHackUrl = function () {
            strataService.users.chatSession.post().then(angular.bind(this, function (sessionId) {
                var url = CHAT_SUPPORT.chatIframeHackUrlPrefix + '?sessionId=' + sessionId + '&ssoName=' + securityService.loginStatus.authedUser.sso_username;
                $scope.chatHackUrl = $sce.trustAsResourceUrl(url);
            }), function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
        $scope.enableChat = function () {
            $scope.showChat = securityService.loginStatus.isLoggedIn && securityService.loginStatus.authedUser.has_chat && CHAT_SUPPORT.enableChat;
            return $scope.showChat;
        };
        $scope.showChat = false;
        // determines whether we should show buttons at all
        $scope.chatAvailable = false;
        //Availability of chat as determined by live agent, toggles chat buttons
        $scope.initializeChat = function () {
            if (!$scope.enableChat() || window.chatInitialized === true) {
                //function should only be called when chat is enabled, and only once per page load
                return;
            }
            if (!window._laq) {
                window._laq = [];
            }
            window._laq.push(function () {
                liveagent.showWhenOnline(CHAT_SUPPORT.chatButtonToken, window.fakeOnlineButton);
                liveagent.showWhenOffline(CHAT_SUPPORT.chatButtonToken, window.fakeOfflineButton);
            });
            //var chatToken = securityService.loginStatus.sessionId;
            var ssoName = securityService.loginStatus.authedUser.sso_username;
            var name = securityService.loginStatus.authedUser.loggedInUser;
            //var currentCaseNumber;
            var accountNumber = securityService.loginStatus.authedUser.account_number;
            // if (currentCaseNumber) {
            //   liveagent
            //     .addCustomDetail('Case Number', currentCaseNumber)
            //     .map('Case', 'CaseNumber', false, false, false)
            //     .saveToTranscript('CaseNumber__c');
            // }
            // if (chatToken) {
            //   liveagent
            //     .addCustomDetail('Session ID', chatToken)
            //     .map('Contact', 'SessionId__c', false, false, false);
            // }
            liveagent.addCustomDetail('Contact Login', ssoName).map('Contact', 'SSO_Username__c', true, true, true).saveToTranscript('SSO_Username__c');
            //liveagent
            //  .addCustomDetail('Contact E-mail', email)
            //  .map('Contact', 'Email', false, false, false);
            liveagent.addCustomDetail('Account Number', accountNumber).map('Account', 'AccountNumber', true, true, true);
            liveagent.setName(name);
            liveagent.addCustomDetail('Name', name);
            liveagent.setChatWindowHeight('552');
            //liveagent.enableLogging();
            liveagent.init(CHAT_SUPPORT.chatLiveAgentUrlPrefix, CHAT_SUPPORT.chatInitHashOne, CHAT_SUPPORT.chatInitHashTwo);
            window.chatInitialized = true;
        };
        $scope.openChatWindow = function () {
            liveagent.startChat(CHAT_SUPPORT.chatButtonToken);
        };
        $scope.init = function () {
            if ($scope.enableChat()) {
                $scope.setChatIframeHackUrl();
                $scope.timer = $interval($scope.checkChatButtonStates, 5000);
                $scope.initializeChat();
            }
        };
        $scope.$on('$destroy', function () {
            //we cancel timer each time scope is destroyed
            //it will be restarted via init on state change to a page that has a chat buttom
            $interval.cancel($scope.timer);
        });
        if (securityService.loginStatus.isLoggedIn) {
            $scope.init();
        } else {
            $scope.authEventLoginSuccess = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
                $scope.init();
            });
            $scope.$on('$destroy', function () {
                $scope.authEventLoginSuccess();
            });
        }

        $scope.$on('$destroy', function () {
            window._laq = null;
        });
    }
]);

'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('CommentsSection', [
    '$scope',
    'CaseService',
    'strataService',
    '$stateParams',
    'AlertService',
    '$modal',
    '$location',
    '$anchorScroll',
    'RHAUtils',
    function ($scope, CaseService, strataService, $stateParams, AlertService, $modal, $location, $anchorScroll, RHAUtils) {
        $scope.CaseService = CaseService;

        CaseService.populateComments($stateParams.id).then(function (comments) {
            $scope.$on('rhaCaseSettled', function() {
                $scope.$evalAsync(function() {
                    CaseService.scrollToComment($location.search().commentId);
                });
            });
        });
        $scope.requestManagementEscalation = function () {
            $modal.open({
                templateUrl: 'cases/views/requestManagementEscalationModal.html',
                controller: 'RequestManagementEscalationModal'
            });
        };
    }
]);

'use strict';
angular.module('RedhatAccess.cases').controller('CompactCaseList', [
    '$scope',
    '$stateParams',
    'strataService',
    'CaseService',
    '$rootScope',
    'AUTH_EVENTS',
    'securityService',
    'SearchCaseService',
    'AlertService',
    'SearchBoxService',
    'RHAUtils',
    '$filter',
    function ($scope, $stateParams, strataService, CaseService, $rootScope, AUTH_EVENTS, securityService, SearchCaseService, AlertService, SearchBoxService, RHAUtils, $filter) {
        $scope.securityService = securityService;
        $scope.CaseService = CaseService;
        $scope.selectedCaseIndex = -1;
        $scope.SearchCaseService = SearchCaseService;
        $scope.selectCase = function ($index) {
            if ($scope.selectedCaseIndex !== $index) {
                $scope.selectedCaseIndex = $index;
            }
        };
        $scope.domReady = false;
        //used to notify resizable directive that the page has loaded
        SearchBoxService.doSearch = CaseService.onSelectChanged = CaseService.onOwnerSelectChanged = CaseService.onGroupSelectChanged = function () {
            SearchCaseService.doFilter().then(function () {
                if (RHAUtils.isNotEmpty($stateParams.id) && $scope.selectedCaseIndex === -1) {
                    var selectedCase = $filter('filter')(SearchCaseService.cases, { 'case_number': $stateParams.id });
                    $scope.selectedCaseIndex = SearchCaseService.cases.indexOf(selectedCase[0]);
                }
                $scope.domReady = true;
            });
        };
        if (securityService.loginStatus.isLoggedIn) {
            CaseService.populateGroups();
            SearchBoxService.doSearch();
        }
        $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            CaseService.populateGroups();
            SearchBoxService.doSearch();
            AlertService.clearAlerts();
        });
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('CompactEdit', [
    '$scope',
    'strataService',
    '$stateParams',
    'CaseService',
    'AttachmentsService',
    '$rootScope',
    'AUTH_EVENTS',
    'CASE_EVENTS',
    'securityService',
    'AlertService',
    function ($scope, strataService, $stateParams, CaseService, AttachmentsService, $rootScope, AUTH_EVENTS, CASE_EVENTS, securityService, AlertService) {
        $scope.securityService = securityService;
        $scope.caseLoading = true;
        $scope.domReady = false;
        $scope.init = function () {
            strataService.cases.get($stateParams.id).then(function (resp) {
                var caseJSON = resp[0];
                var cacheHit = resp[1];
                if (!cacheHit) {
                    CaseService.defineCase(caseJSON);
                } else {
                    CaseService.kase = caseJSON;
                }
                $rootScope.$broadcast(CASE_EVENTS.received);
                $scope.caseLoading = false;
                if (caseJSON.product !== undefined && caseJSON.product.name !== undefined) {
                    strataService.products.versions(caseJSON.product.name).then(function (versions) {
                        CaseService.versions = versions;
                    }, function (error) {
                        AlertService.addStrataErrorMessage(error);
                    });
                }
                $scope.domReady = true;
            });
            strataService.cases.attachments.list($stateParams.id).then(function (attachmentsJSON) {
                AttachmentsService.defineOriginalAttachments(attachmentsJSON);
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
        if (securityService.loginStatus.isLoggedIn) {
            $scope.init();
        }
        $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            $scope.init();
            AlertService.clearAlerts();
        });
    }
]);
'use strict';
/*global $ */
angular.module('RedhatAccess.cases').controller('CreateGroupButton', [
    '$scope',
    '$modal',
    function ($scope, $modal) {
        $scope.openCreateGroupDialog = function () {
            $modal.open({
                templateUrl: 'cases/views/createGroupModal.html',
                controller: 'CreateGroupModal'
            });
        };
    }
]);
'use strict';
/*global $ */
angular.module('RedhatAccess.cases').controller('CreateGroupModal', [
    '$scope',
    '$modalInstance',
    'strataService',
    'AlertService',
    'CaseService',
    'GroupService',
    function ($scope, $modalInstance, strataService, AlertService, CaseService, GroupService) {
        $scope.createGroup = function () {
            AlertService.addWarningMessage('Creating group ' + this.groupName + '...');
            $modalInstance.close();
            strataService.groups.create(this.groupName).then(angular.bind(this, function (success) {
                CaseService.groups.push({
                    name: this.groupName,
                    number: success
                });
                AlertService.clearAlerts();
                AlertService.addSuccessMessage('Successfully created group ' + this.groupName);
                GroupService.reloadTable();
            }), function (error) {
                AlertService.clearAlerts();
                AlertService.addStrataErrorMessage(error);
            });
        };
        $scope.closeModal = function () {
            $modalInstance.close();
        };
        $scope.onGroupNameKeyPress = function ($event) {
            if ($event.keyCode === 13) {
                angular.bind(this, $scope.createGroup)();
            }
        };
    }
]);
'use strict';
/*global $ */
/*jshint expr: true, camelcase: false, newcap: false */
angular.module('RedhatAccess.cases').controller('DefaultGroup', [
    '$scope',
    '$rootScope',
    'strataService',
    'AlertService',
    '$location',
    'securityService',
    'AUTH_EVENTS',
    function ($scope, $rootScope, strataService, AlertService, $location, securityService, AUTH_EVENTS) {
        $scope.securityService = securityService;
        $scope.listEmpty = false;
        $scope.selectedGroup = {};
        $scope.selectedUser = '';
        $scope.usersOnAccount = [];
        $scope.account = null;
        $scope.groups = [];
        $scope.ssoName = null;
        $scope.groupsLoading = false;
        $scope.usersLoading = false;
        $scope.usersLoaded = false;
        $scope.usersAndGroupsFinishedLoading = false;
        
        $scope.init = function() {
            if(securityService.userAllowedToManageGroups()){
                $scope.groupsLoading = true;
                var loc = $location.url().split('/');
                $scope.ssoName = securityService.loginStatus.authedUser.sso_username;
                $scope.account = securityService.loginStatus.account;
                strataService.groups.list($scope.ssoName).then(function (groups) {
                    $scope.groupsLoading = false;
                    $scope.groups = groups;
                }, function (error) {
                    $scope.groupsLoading = false;
                    AlertService.addStrataErrorMessage(error);
                });

                $scope.usersLoading = true;
                strataService.accounts.users($scope.account.number, $scope.selectedGroup.number).then(function (users) {
                    $scope.usersLoading = false;
                    $scope.usersOnAccount = users;
                    $scope.usersLoaded = true;
                }, function (error) {
                    $scope.usersLoading = false;
                    AlertService.addStrataErrorMessage(error);
                });
            }else{
                $scope.usersLoading = false;
                $scope.groupsLoading = false;
                AlertService.addStrataErrorMessage('User does not have proper credentials to manage default groups.');
            }
        };

        $scope.userChange = function (){
            $scope.usersAndGroupsFinishedLoading = true;
        };

        $scope.setDefaultGroup = function () {
            //Remove old group is_default
            var tmpGroup = {
                name: $scope.selectedGroup.name,
                number: $scope.selectedGroup.number,
                isDefault: true,
                contactSsoName: $scope.selectedUser.sso_username
            };
            strataService.groups.createDefault(tmpGroup).then(function () {
                AlertService.addSuccessMessage('Successfully set ' + tmpGroup.name + ' as ' + $scope.selectedUser.sso_username + '\'s default group.');
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };

        $scope.back = function(){
            $location.path('/case/group');
        };

        if (securityService.loginStatus.isLoggedIn) {
            $scope.init();

        }
        $scope.authEventLogin = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            $scope.init();
        });

        $scope.authEventLogoutSuccess = $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            $scope.selectedGroup = {};
            $scope.usersOnScreen = [];
            $scope.usersOnAccount = [];
            $scope.accountNumber = null;
        });

        $scope.$on('$destroy', function () {
            $scope.authEventLogoutSuccess();
            $scope.authEventLogin();
        });
    }
]);
'use strict';
/*global $ */
angular.module('RedhatAccess.cases').controller('DeleteGroupButton', [
    '$scope',
    'strataService',
    'AlertService',
    'CaseService',
    '$q',
    '$filter',
    'GroupService',
    function ($scope, strataService, AlertService, CaseService, $q, $filter, GroupService) {
        $scope.GroupService = GroupService;
        $scope.deleteGroups = function () {
            var promises = [];
            angular.forEach(CaseService.groups, function (group, index) {
                if (group.selected) {
                    var promise = strataService.groups.remove(group.number);
                    promise.then(function (success) {
                        var groups = $filter('filter')(CaseService.groups, function (g) {
                                if (g.number !== group.number) {
                                    return true;
                                } else {
                                    return false;
                                }
                            });
                        CaseService.groups = groups;
                        GroupService.reloadTable();
                        AlertService.addSuccessMessage('Successfully deleted group ' + group.name);
                    }, function (error) {
                        AlertService.addStrataErrorMessage(error);
                    });
                    promises.push(promise);
                }
            });
            AlertService.addWarningMessage('Deleting groups...');
            var parentPromise = $q.all(promises);
            parentPromise.then(function (success) {
                AlertService.clearAlerts();
                AlertService.addSuccessMessage('Successfully deleted groups.');
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('DescriptionSection', [
    '$scope',
    'CaseService',
    function ($scope, CaseService) {
        $scope.CaseService = CaseService;
    }
]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('DetailsSection', [
    '$scope',
    'strataService',
    'CaseService',
    '$rootScope',
    'AUTH_EVENTS',
    'CASE_EVENTS',
    'AlertService',
    'RHAUtils',
    function ($scope, strataService, CaseService, $rootScope, AUTH_EVENTS, CASE_EVENTS, AlertService, RHAUtils) {
        $scope.CaseService = CaseService;
        $scope.init = function () {
            if (!$scope.compact) {
                strataService.values.cases.types().then(function (response) {
                    $scope.caseTypes = response;
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                });
                strataService.groups.list(CaseService.kase.contact_sso_username).then(function (response) {
                    $scope.groups = response;
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                });
            }
            strataService.values.cases.status().then(function (response) {
                $scope.statuses = response;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            strataService.values.cases.severity().then(function (response) {
                CaseService.severities = response;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            strataService.products.list().then(function (response) {
                $scope.products = response;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
        $scope.updatingDetails = false;
        $scope.updateCase = function () {
            $scope.updatingDetails = true;
            var caseJSON = {};
            if (CaseService.kase !== undefined) {
                if (CaseService.kase.type !== undefined) {
                    caseJSON.type = CaseService.kase.type.name;
                }
                if (CaseService.kase.severity !== undefined) {
                    caseJSON.severity = CaseService.kase.severity.name;
                }
                if (CaseService.kase.status !== undefined) {
                    caseJSON.status = CaseService.kase.status.name;
                }
                if (CaseService.kase.alternate_id !== undefined) {
                    caseJSON.alternateId = CaseService.kase.alternate_id;
                }
                if (CaseService.kase.product !== undefined) {
                    caseJSON.product = CaseService.kase.product.name;
                }
                if (CaseService.kase.version !== undefined) {
                    caseJSON.version = CaseService.kase.version;
                }
                if (CaseService.kase.summary !== undefined) {
                    caseJSON.summary = CaseService.kase.summary;
                }
                if (CaseService.kase.group !== null && CaseService.kase.group !== undefined && CaseService.kase.group.number !== undefined) {
                    caseJSON.folderNumber = CaseService.kase.group.number;
                } else {
                    caseJSON.folderNumber = '';
                }
                if (RHAUtils.isNotEmpty(CaseService.kase.fts)) {
                    caseJSON.fts = CaseService.kase.fts;
                    if (!CaseService.kase.fts) {
                        caseJSON.contactInfo24X7 = '';
                    }
                }
                if (CaseService.kase.fts && RHAUtils.isNotEmpty(CaseService.kase.contact_info24_x7)) {
                    caseJSON.contactInfo24X7 = CaseService.kase.contact_info24_x7;
                }
                if (CaseService.kase.notes !== null) {
                    caseJSON.notes = CaseService.kase.notes;
                }
                strataService.cases.put(CaseService.kase.case_number, caseJSON).then(function () {
                    $scope.caseDetails.$setPristine();
                    $scope.updatingDetails = false;
                    if ($scope.$root.$$phase !== '$apply' && $scope.$root.$$phase !== '$digest') {
                        $scope.$apply();
                    }
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                    $scope.updatingDetails = false;
                    $scope.$apply();
                });
            }
        };
        $scope.getProductVersions = function () {
            CaseService.versions = [];
            strataService.products.versions(CaseService.kase.product.code).then(function (versions) {
                CaseService.versions = versions;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
        if (CaseService.caseDataReady) {
            $scope.init();
        }
        $scope.caseEventDeregister = $rootScope.$on(CASE_EVENTS.received, function () {
            $scope.init();
            AlertService.clearAlerts();
        });
        $scope.$on('$destroy', function () {
            $scope.caseEventDeregister();
        });
    }
]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('Edit', [
    '$scope',
    '$stateParams',
    '$filter',
    '$q',
    'AttachmentsService',
    'CaseService',
    'strataService',
    'RecommendationsService',
    '$rootScope',
    'AUTH_EVENTS',
    'AlertService',
    'securityService',
    'EDIT_CASE_CONFIG',
    'RHAUtils',
    'CASE_EVENTS',
    function ($scope, $stateParams, $filter, $q, AttachmentsService, CaseService, strataService, RecommendationsService, $rootScope, AUTH_EVENTS, AlertService, securityService, EDIT_CASE_CONFIG, RHAUtils, CASE_EVENTS) {
        $scope.EDIT_CASE_CONFIG = EDIT_CASE_CONFIG;
        $scope.securityService = securityService;
        $scope.AttachmentsService = AttachmentsService;
        $scope.CaseService = CaseService;
        CaseService.clearCase();
        $scope.loading = {};
        $scope.init = function () {
            $scope.loading.kase = true;
            $scope.recommendationsLoading = true;
            strataService.cases.get($stateParams.id).then(function (resp) {
                var caseJSON = resp[0];
                var cacheHit = resp[1];
                if (!cacheHit) {
                    CaseService.defineCase(caseJSON);
                } else {
                    CaseService.setCase(caseJSON);
                }
                $rootScope.$broadcast(CASE_EVENTS.received);
                $scope.loading.kase = false;
                if ('product' in caseJSON && 'name' in caseJSON.product && caseJSON.product.name) {
                    strataService.products.versions(caseJSON.product.name).then(function (versions) {
                        CaseService.versions = versions;
                    }, function (error) {
                        AlertService.addStrataErrorMessage(error);
                    });
                }
                if (caseJSON.account_number !== undefined) {
                    strataService.accounts.get(caseJSON.account_number).then(function (account) {
                        CaseService.defineAccount(account);
                    }, function (error) {
                        AlertService.addStrataErrorMessage(error);
                    });
                }
                if (EDIT_CASE_CONFIG.showRecommendations) {
                    var pinnedDfd = RecommendationsService.populatePinnedRecommendations().then(angular.noop, function (error) {
                        AlertService.addStrataErrorMessage(error);
                    });
                    var reccomendDfd = RecommendationsService.populateRecommendations(12);
                    $q.all([pinnedDfd, reccomendDfd]).then(function(){
                        $scope.recommendationsLoading = false;
                    });
                }
                if (EDIT_CASE_CONFIG.showEmailNotifications && !cacheHit) {
                    CaseService.defineNotifiedUsers();
                }
            });
            if (EDIT_CASE_CONFIG.showAttachments) {
                $scope.loading.attachments = true;
                strataService.cases.attachments.list($stateParams.id).then(function (attachmentsJSON) {
                    AttachmentsService.defineOriginalAttachments(attachmentsJSON);
                    $scope.loading.attachments= false;
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                    $scope.loading.attachments= false;
                });
            }
            if (EDIT_CASE_CONFIG.showComments) {
                $scope.loading.comments = true;
                strataService.cases.comments.get($stateParams.id).then(function (commentsJSON) {
                    $scope.comments = commentsJSON;
                    $scope.loading.comments = false;
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                    $scope.loading.comments = false;
                });
            }
        };
        if (securityService.loginStatus.isLoggedIn) {
            $scope.init();
        }
        $scope.authLoginEvent = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            $scope.init();
            AlertService.clearAlerts();
        });

        var caseSettled = function() {
            $scope.$broadcast('rhaCaseSettled');
        };

        $scope.loadingWatcher = $scope.$watch('loading', function(loadingObj){
            if($.isEmptyObject(loadingObj)) {
                return;
            }
            var allLoaded = true;
            for (var key in loadingObj) {
                if(loadingObj[key] !== false) {
                    allLoaded = false;
                }
            }
            if(allLoaded) {
                caseSettled();
            }
        }, true);

        $scope.loadingRecWatcher = $scope.$watch('recommendationsLoading', function(newVal) {
            if(newVal === false) {
                caseSettled();
            }
        });

        $scope.$on('$destroy', function () {
            // Clean up listeners
            CaseService.clearCase();
            $scope.authLoginEvent();
            $scope.loadingWatcher();
            $scope.loadingRecWatcher();
        });
    }
]);

/*global angular*/
/*jshint camelcase: false*/
'use strict';
angular.module('RedhatAccess.cases').controller('EmailNotifySelect', [
    '$scope',
    '$rootScope',
    'CaseService',
    'securityService',
    'AlertService',
    'strataService',
    '$filter',
    'RHAUtils',
    'EDIT_CASE_CONFIG',
    'AUTH_EVENTS',
    function ($scope, $rootScope, CaseService, securityService, AlertService, strataService, $filter, RHAUtils, EDIT_CASE_CONFIG, AUTH_EVENTS) {
        $scope.securityService = securityService;
        $scope.CaseService = CaseService;
        $scope.showEmailNotifications = EDIT_CASE_CONFIG.showEmailNotifications;
        $scope.updateNotifyUsers = function () {
            if (!angular.equals(CaseService.updatedNotifiedUsers, CaseService.originalNotifiedUsers)) {
                angular.forEach(CaseService.originalNotifiedUsers, function (origUser) {
                    var updatedUser = $filter('filter')(CaseService.updatedNotifiedUsers, origUser);
                    if (RHAUtils.isEmpty(updatedUser)) {
                        $scope.updatingList = true;
                        strataService.cases.notified_users.remove(CaseService.kase.case_number, origUser).then(function () {
                            $scope.updatingList = false;
                            CaseService.originalNotifiedUsers = CaseService.updatedNotifiedUsers;
                        }, function (error) {
                            $scope.updatingList = false;
                            AlertService.addStrataErrorMessage(error);
                        });
                    }
                });
                angular.forEach(CaseService.updatedNotifiedUsers, function (updatedUser) {
                    var originalUser = $filter('filter')(CaseService.originalNotifiedUsers, updatedUser);
                    if (RHAUtils.isEmpty(originalUser)) {
                        $scope.updatingList = true;
                        strataService.cases.notified_users.add(CaseService.kase.case_number, updatedUser).then(function () {
                            CaseService.originalNotifiedUsers = CaseService.updatedNotifiedUsers;
                            $scope.updatingList = false;
                        }, function (error) {
                            $scope.updatingList = false;
                            AlertService.addStrataErrorMessage(error);
                        });
                    }
                });
            }
        };
        if (securityService.loginStatus.isLoggedIn) {
            CaseService.populateUsers();
        }
        $scope.authEventDeregister = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            CaseService.populateUsers();
        });
        $scope.$on('$destroy', function () {
            $scope.authEventDeregister();
        });
    }
]);

'use strict';
angular.module('RedhatAccess.cases').controller('EntitlementSelect', [
    '$scope',
    'strataService',
    'AlertService',
    '$filter',
    'RHAUtils',
    'CaseService',
    function ($scope, strataService, AlertService, $filter, RHAUtils, CaseService) {
        $scope.CaseService = CaseService;
        CaseService.populateEntitlements();
    }
]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('ExportCSVButton', [
    '$scope',
    'strataService',
    'AlertService',
    function ($scope, strataService, AlertService) {
        $scope.exporting = false;
        $scope.exports = function () {
            $scope.exporting = true;
            strataService.cases.csv().then(function (response) {
                $scope.exporting = false;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
    }
]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('Group', [
    '$scope',
    '$location',
    'securityService',
    'SearchBoxService',
    'GroupService',
    function ($scope, $location, securityService, SearchBoxService, GroupService) {
        $scope.securityService = securityService;
        $scope.onChange = SearchBoxService.onChange = SearchBoxService.doSearch = SearchBoxService.onKeyPress = function () {
            GroupService.reloadTable();
        };
        $scope.$on('$destroy', function () {
            $scope.onChange();
        });
        $scope.defaultCaseGroup = function(){
            $location.path('/case/group/default');
        };
    }
]);
'use strict';
/*global $ */
/*jshint expr: true, camelcase: false, newcap: false*/
angular.module('RedhatAccess.cases').controller('GroupList', [
    '$rootScope',
    '$scope',
    'strataService',
    'AlertService',
    'CaseService',
    '$filter',
    'ngTableParams',
    'GroupService',
    'securityService',
    'SearchBoxService',
    'AUTH_EVENTS',
    function ($rootScope, $scope, strataService, AlertService, CaseService, $filter, ngTableParams, GroupService, securityService, SearchBoxService, AUTH_EVENTS) {
        $scope.CaseService = CaseService;
        $scope.GroupService = GroupService;
        $scope.listEmpty = false;
        $scope.groupsOnScreen = [];
        $scope.canManageGroups = false;
        var reloadTable = false;
        var tableBuilt = false;
        $scope.groupsLoading = true;
        var buildTable = function () {
            $scope.tableParams = new ngTableParams({
                page: 1,
                count: 10,
                sorting: { name: 'asc' }
            }, {
                total: CaseService.groups.length,
                getData: function ($defer, params) {
                    var orderedData = $filter('filter')(CaseService.groups, SearchBoxService.searchTerm);
                    orderedData = params.sorting() ? $filter('orderBy')(orderedData, params.orderBy()) : orderedData;
                    orderedData.length < 1 ? $scope.listEmpty = true : $scope.listEmpty = false;
                    var pageData = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                    $scope.tableParams.total(orderedData.length);
                    GroupService.groupsOnScreen = pageData;
                    $defer.resolve(pageData);
                }
            });
            $scope.tableParams.settings().$scope = $scope;
            GroupService.reloadTable = function () {
                $scope.tableParams.reload();
            };
            tableBuilt = true;
        };
        
        $scope.onMasterCheckboxClicked = function () {
            for (var i = 0; i < GroupService.groupsOnScreen.length; i++) {
                if (this.masterSelected) {
                    GroupService.groupsOnScreen[i].selected = true;
                    GroupService.disableDeleteGroup = false;
                } else {
                    GroupService.groupsOnScreen[i].selected = false;
                    GroupService.disableDeleteGroup = true;
                }
            }
        };
        CaseService.clearCase();

        $scope.init = function() {
            strataService.groups.list().then(function (groups) {
                CaseService.groups = groups;
                $scope.canManageGroups = securityService.loginStatus.account.has_group_acls && securityService.loginStatus.authedUser.org_admin;
                $scope.groupsLoading = false;
                buildTable();
                if(reloadTable){
                    //GroupService.reloadTable();
                    reloadTable = false;
                }
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };

        $scope.onGroupSelected = function() {
            var disableDeleteGroup = true;
            for (var i = 0; i < GroupService.groupsOnScreen.length; i++) {
                if (GroupService.groupsOnScreen[i].selected === true) {
                    disableDeleteGroup = false;
                    break;
                }
            }
            GroupService.disableDeleteGroup = disableDeleteGroup;
        };

        if (securityService.loginStatus.isLoggedIn) {
            $scope.init();

        }
        $scope.authEventLogin = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            $scope.init();
        });

        $scope.authEventLogoutSuccess = $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            CaseService.clearCase();
            $scope.groupsOnScreen = [];
            GroupService.groupsOnScreen = [];
            reloadTable = true;
        });

        $scope.$on('$destroy', function () {
            CaseService.clearCase();
            $scope.authEventLogoutSuccess();
            $scope.authEventLogin();
        });
    }
]);

/*jshint camelcase: false */
'use strict';
angular.module('RedhatAccess.cases').constant('CASE_GROUPS', {
    manage: 'manage',
    ungrouped: 'ungrouped'
}).controller('GroupSelect', [
    '$scope',
    'securityService',
    'SearchCaseService',
    'CaseService',
    'strataService',
    'AlertService',
    'CASE_GROUPS',
    'AUTH_EVENTS',
    function ($scope, securityService, SearchCaseService, CaseService, strataService, AlertService, CASE_GROUPS, AUTH_EVENTS) {
        $scope.securityService = securityService;
        $scope.SearchCaseService = SearchCaseService;
        $scope.CaseService = CaseService;
        $scope.CASE_GROUPS = CASE_GROUPS;

        $scope.setSearchOptions = function (showsearchoptions) {
            CaseService.showsearchoptions = showsearchoptions;
            CaseService.buildGroupOptions();
        };
    }
]);

'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('List', [
    '$scope',
    '$filter',
    'ngTableParams',
    'securityService',
    'AlertService',
    '$rootScope',
    'SearchCaseService',
    'CaseService',
    'AUTH_EVENTS',
    'SearchBoxService',
    function ($scope, $filter, ngTableParams, securityService, AlertService, $rootScope, SearchCaseService, CaseService, AUTH_EVENTS, SearchBoxService) {
        $scope.SearchCaseService = SearchCaseService;
        $scope.securityService = securityService;
        $scope.AlertService = AlertService;
        AlertService.clearAlerts();
        var tableBuilt = false;
        var buildTable = function () {
            /*jshint newcap: false*/
            $scope.tableParams = new ngTableParams({
                page: 1,
                count: 10,
                sorting: { last_modified_date: 'desc' }
            }, {
                total: SearchCaseService.totalCases,
                getData: function ($defer, params) {
                    if (!SearchCaseService.allCasesDownloaded && params.count() * params.page() >= SearchCaseService.count) {
                        SearchCaseService.doFilter().then(function () {
                            $scope.tableParams.reload();
                            var orderedData = params.sorting() ? $filter('orderBy')(SearchCaseService.cases, params.orderBy()) : SearchCaseService.cases;
                            var pageData = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                            $scope.tableParams.total(SearchCaseService.totalCases);
                            $defer.resolve(pageData);
                        });
                    } else {
                        var orderedData = params.sorting() ? $filter('orderBy')(SearchCaseService.cases, params.orderBy()) : SearchCaseService.cases;
                        var pageData = orderedData.slice((params.page() - 1) * params.count(), params.page() * params.count());
                        $scope.tableParams.total(SearchCaseService.totalCases);
                        $defer.resolve(pageData);
                    }
                }
            });
            tableBuilt = true;
        };
        SearchBoxService.doSearch = CaseService.onSelectChanged = CaseService.onOwnerSelectChanged = CaseService.onGroupSelectChanged = function () {
            SearchCaseService.clearPagination();
            if(CaseService.groups.length === 0){
                CaseService.populateGroups().then(function (){
                    SearchCaseService.doFilter().then(function () {
                        if (!tableBuilt) {
                            buildTable();
                        } else {
                            $scope.tableParams.reload();
                        }
                    });
                });
            } else {
                //CaseService.buildGroupOptions();
                SearchCaseService.doFilter().then(function () {
                    if (!tableBuilt) {
                        buildTable();
                    } else {
                        $scope.tableParams.reload();
                    }
                });
            }
            
        };
        /**
       * Callback after user login. Load the cases and clear alerts
       */
        if (securityService.loginStatus.isLoggedIn && securityService.loginStatus.userAllowedToManageCases) {
            SearchCaseService.clear();
            SearchBoxService.doSearch();
        }
        $scope.listAuthEventDeregister = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            if(securityService.loginStatus.userAllowedToManageCases){
                SearchBoxService.doSearch();
                AlertService.clearAlerts();
            }
        });

        $scope.authEventLogoutSuccess = $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            CaseService.clearCase();
            SearchCaseService.clear();
        });
        
        $scope.$on('$destroy', function () {
            $scope.listAuthEventDeregister();
            CaseService.clearCase();
        });
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('ListAttachments', [
    '$scope',
    'AttachmentsService',
    function ($scope, AttachmentsService) {
        $scope.AttachmentsService = AttachmentsService;
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('ListBugzillas', [
    '$scope',
    'CaseService',
    'securityService',
    function ($scope, CaseService, securityService) {
        $scope.CaseService = CaseService;
        $scope.securityService = securityService;
    }
]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').controller('ListFilter', [
    '$scope',
    'STATUS',
    'CaseService',
    'securityService',
    function ($scope, STATUS, CaseService, securityService) {
        $scope.securityService = securityService;
        CaseService.status = STATUS.both;
        $scope.showsearchoptions = CaseService.showsearchoptions;
        $scope.setSearchOptions = function (showsearchoptions) {
            CaseService.showsearchoptions = showsearchoptions;
            if(CaseService.groups.length === 0){
                CaseService.populateGroups().then(function (){
                    CaseService.buildGroupOptions();
                });
            } else{
                CaseService.buildGroupOptions();
            }
        };
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('ListNewAttachments', [
    '$scope',
    'AttachmentsService',
    'TreeViewSelectorUtils',
    function ($scope, AttachmentsService, TreeViewSelectorUtils) {
        $scope.AttachmentsService = AttachmentsService;
        $scope.TreeViewSelectorUtils = TreeViewSelectorUtils;
        $scope.removeLocalAttachment = function ($index) {
            AttachmentsService.removeUpdatedAttachment($index);
        };
    }
]);
'use strict';
/*jshint camelcase: false*/
angular.module('RedhatAccess.cases').controller('New', [
    '$scope',
    '$state',
    '$q',
    '$timeout',
    'SearchResultsService',
    'AttachmentsService',
    'strataService',
    'RecommendationsService',
    'CaseService',
    'AlertService',
    'securityService',
    '$rootScope',
    'AUTH_EVENTS',
    '$location',
    'RHAUtils',
    'NEW_DEFAULTS',
    'NEW_CASE_CONFIG',
    '$http',
    function ($scope, $state, $q, $timeout, SearchResultsService, AttachmentsService, strataService, RecommendationsService, CaseService, AlertService, securityService, $rootScope, AUTH_EVENTS, $location, RHAUtils, NEW_DEFAULTS, NEW_CASE_CONFIG, $http) {
        $scope.NEW_CASE_CONFIG = NEW_CASE_CONFIG;
        $scope.versions = [];
        $scope.versionDisabled = true;
        $scope.versionLoading = false;
        $scope.incomplete = true;
        $scope.submitProgress = 0;
        AttachmentsService.clear();
        CaseService.clearCase();
        RecommendationsService.clear();
        SearchResultsService.clear();
        AlertService.clearAlerts();
        $scope.CaseService = CaseService;
        $scope.RecommendationsService = RecommendationsService;
        $scope.securityService = securityService;

        // Instantiate these variables outside the watch
        var waiting = false;
        $scope.$watch('CaseService.kase.description + CaseService.kase.summary', function () {
            if (!waiting){
                waiting = true;
                $timeout(function() {
                    waiting = false;
                    $scope.getRecommendations();
                }, 500); // delay 500 ms
            }
        });

        $scope.getRecommendations = function () {
            if ($scope.NEW_CASE_CONFIG.showRecommendations) {
                SearchResultsService.searchInProgress.value = true;
                var numRecommendations = 5;
                if($scope.NEW_CASE_CONFIG.isPCM){
                    numRecommendations = 30;
                }
                RecommendationsService.populateRecommendations(numRecommendations).then(function () {
                    SearchResultsService.clear();
                    RecommendationsService.recommendations.forEach(function (recommendation) {
                        SearchResultsService.add(recommendation);
                    });
                    SearchResultsService.searchInProgress.value = false;
                }, function (error) {
                    AlertService.addStrataErrorMessage(error);
                    SearchResultsService.searchInProgress.value = false;
                });
            }
        };
        CaseService.onOwnerSelectChanged = function () {
            if (CaseService.owner !== undefined) {
                CaseService.populateEntitlements(CaseService.owner);
                CaseService.populateGroups(CaseService.owner);
            }
            CaseService.validateNewCasePage1();
        };

        /**
        * Add the top sorted products to list
        */
        $scope.buildProductOptions = function(originalProductList) {
            var productOptions = [];
            var productSortList = [];
            if($scope.NEW_CASE_CONFIG.isPCM){
                $http.get($scope.NEW_CASE_CONFIG.productSortListFile).then(function (response) {
                    if (response.status === 200 && response.data !== undefined) {
                        productSortList = response.data.split(',');

                        for(var i = 0; i < productSortList.length; i++) {
                            for (var j = 0 ; j < originalProductList.length ; j++) {
                                if (productSortList[i] === originalProductList[j].code) {
                                    var sortProduct = productSortList[i];
                                    productOptions.push({
                                        value: sortProduct,
                                        label: sortProduct
                                    });
                                    break;
                                }
                            }
                        }

                        var sep = '────────────────────────────────────────';
                        if (productOptions.length > 0) {
                            productOptions.push({
                                isDisabled: true,
                                label: sep
                            });
                        }

                        angular.forEach(originalProductList, function(product){
                            productOptions.push({
                                value: product.code,
                                label: product.name
                            });
                        }, this);

                        $scope.products = productOptions;
                    } else {
                        angular.forEach(originalProductList, function(product){
                            productOptions.push({
                                value: product.code,
                                label: product.name
                            });
                        }, this);
                        $scope.products = productOptions;
                    }
                });
            } else {
                angular.forEach(originalProductList, function(product){
                    productOptions.push({
                        value: product.code,
                        label: product.name
                    });
                }, this);
                $scope.products = productOptions;
            }
        };

        /**
       * Populate the selects
       */
        $scope.initSelects = function () {
            CaseService.clearCase();
            $scope.productsLoading = true;
            strataService.products.list(securityService.loginStatus.authedUser.sso_username).then(function (products) {
                $scope.buildProductOptions(products);
                $scope.productsLoading = false;
                if (RHAUtils.isNotEmpty(NEW_DEFAULTS.product)) {
                    CaseService.kase.product = {
                        name: NEW_DEFAULTS.product,
                        code: NEW_DEFAULTS.product
                    };
                    $scope.getRecommendations();
                    $scope.getProductVersions(CaseService.kase.product);
                }
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            $scope.severitiesLoading = true;
            strataService.values.cases.severity().then(function (severities) {
                CaseService.severities = severities;
                CaseService.kase.severity = severities[severities.length - 1];
                $scope.severitiesLoading = false;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            $scope.groupsLoading = true;
            CaseService.populateGroups().then(function (groups) {
                $scope.groupsLoading = false;
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
        $scope.initDescription = function () {
            var searchObject = $location.search();
            var setDesc = function (desc) {
                CaseService.kase.description = desc;
                $scope.getRecommendations();
            };
            if (searchObject.data) {
                setDesc(searchObject.data);
            } else {
                //angular does not  handle params before hashbang
                //@see https://github.com/angular/angular.js/issues/6172
                var queryParamsStr = window.location.search.substring(1);
                var parameters = queryParamsStr.split('&');
                for (var i = 0; i < parameters.length; i++) {
                    var parameterName = parameters[i].split('=');
                    if (parameterName[0] === 'data') {
                        setDesc(decodeURIComponent(parameterName[1]));
                    }
                }
            }
        };
        if (securityService.loginStatus.isLoggedIn) {
            $scope.initSelects();
            $scope.initDescription();
        }
        $scope.authLoginSuccess = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            $scope.initSelects();
            $scope.initDescription();
            AlertService.clearAlerts();
            RecommendationsService.failureCount = 0;
        });

        $scope.$on('$destroy', function () {
            $scope.authLoginSuccess();
        });
        /**
       * Retrieve product's versions from strata
       *
       * @param product
       */
        $scope.getProductVersions = function (product) {
            CaseService.kase.version = '';
            $scope.versionDisabled = true;
            $scope.versionLoading = true;
            strataService.products.versions(product).then(function (response) {
                $scope.versions = response;
                CaseService.validateNewCasePage1();
                $scope.versionDisabled = false;
                $scope.versionLoading = false;
                if (RHAUtils.isNotEmpty(NEW_DEFAULTS.version)) {
                    CaseService.kase.version = NEW_DEFAULTS.version;
                    $scope.getRecommendations();
                }
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });

            //Retrieve the product detail, basically finding the attachment artifact
            $scope.fetchProductDetail(product);
        };

        /**
        * Fetch the product details for the selected product
        **/
        $scope.fetchProductDetail = function (productCode) {
            AttachmentsService.suggestedArtifact = {};
            strataService.products.get(productCode).then(angular.bind(this, function (product) {
                if (product !== undefined && product.suggested_artifacts !== undefined && product.suggested_artifacts.suggested_artifact !== undefined) {
                    if (product.suggested_artifacts.suggested_artifact.length > 0) {
                        var description = product.suggested_artifacts.suggested_artifact[0].description;
                        if (description.indexOf('<a') > -1) {
                            description = description.replace("<a","<a target='_blank'");
                        }
                        AttachmentsService.suggestedArtifact.description = description;
                    }
                }
            }), function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };

        /**
       * Go to a page in the wizard
       *
       * @param page
       */
        $scope.gotoPage = function (page) {
            $scope.isPage1 = page === 1 ? true : false;
            $scope.isPage2 = page === 2 ? true : false;
        };
        /**
       * Navigate forward in the wizard
       */
        $scope.doNext = function () {
            $scope.gotoPage(2);
        };
        /**
       * Navigate back in the wizard
       */
        $scope.doPrevious = function () {
            $scope.gotoPage(1);
        };
        $scope.submittingCase = false;

        $scope.setSearchOptions = function (showsearchoptions) {
            CaseService.showsearchoptions = showsearchoptions;
            if(CaseService.groups.length === 0){
                CaseService.populateGroups().then(function (){
                    CaseService.buildGroupOptions();
                });
            } else{
                CaseService.buildGroupOptions();
            }
        };
        /**
       * Create the case with attachments
       */
        $scope.doSubmit = function ($event) {
            if (window.chrometwo_require !== undefined) {
                chrometwo_require(['analytics/main'], function (analytics) {
                    analytics.trigger('OpenSupportCaseSubmit', $event);
                });
            }
            /*jshint camelcase: false */
            var caseJSON = {
                    'product': CaseService.kase.product.code,
                    'version': CaseService.kase.version,
                    'summary': CaseService.kase.summary,
                    'description': CaseService.kase.description,
                    'severity': CaseService.kase.severity.name
                };
            if (RHAUtils.isNotEmpty(CaseService.group)) {
                caseJSON.folderNumber = CaseService.group;
            }
            if (RHAUtils.isNotEmpty(CaseService.entitlement)) {
                caseJSON.entitlement = {};
                caseJSON.entitlement.sla = CaseService.entitlement;
            }
            if (RHAUtils.isNotEmpty(CaseService.account)) {
                caseJSON.accountNumber = CaseService.account.number;
            }
            if (CaseService.fts) {
                caseJSON.fts = true;
                if (CaseService.fts_contact) {
                    caseJSON.contactInfo24X7 = CaseService.fts_contact;
                }
            }
            if (RHAUtils.isNotEmpty(CaseService.owner)) {
                caseJSON.contactSsoUsername = CaseService.owner;
            }
            $scope.submittingCase = true;
            AlertService.addWarningMessage('Creating case...');
            var redirectToCase = function (caseNumber) {
                $state.go('edit', { id: caseNumber });
                AlertService.clearAlerts();
                $scope.submittingCase = false;
            };
            strataService.cases.post(caseJSON).then(function (caseNumber) {
                AlertService.clearAlerts();
                AlertService.addSuccessMessage('Successfully created case number ' + caseNumber);
                if ((AttachmentsService.updatedAttachments.length > 0 || AttachmentsService.hasBackEndSelections()) && NEW_CASE_CONFIG.showAttachments) {
                    AttachmentsService.updateAttachments(caseNumber).then(function () {
                        redirectToCase(caseNumber);
                    }, function (error) {
                        AlertService.addStrataErrorMessage(error);
                        $scope.submittingCase = false;
                    });
                } else {
                    redirectToCase(caseNumber);
                }
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
                $scope.submittingCase = false;
            });
        };
        $scope.gotoPage(1);

        $scope.authEventLogoutSuccess = $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            CaseService.clearCase();
        });
        $scope.$on('$destroy', function () {
            CaseService.clearCase();
        });
    }
]);

/*global angular*/
'use strict';
angular.module('RedhatAccess.cases').controller('OwnerSelect', [
    '$scope',
    '$rootScope',
    'securityService',
    'AUTH_EVENTS',
    'SearchCaseService',
    'CaseService',
    function ($scope, $rootScope, securityService, AUTH_EVENTS, SearchCaseService, CaseService) {
        $scope.securityService = securityService;
        $scope.SearchCaseService = SearchCaseService;
        $scope.CaseService = CaseService;
    }
]);

'use strict';
/*jshint camelcase: false, expr: true*/
angular.module('RedhatAccess.cases').controller('PcmRecommendationsController', [
    '$scope',
    '$location',
    'SearchResultsService',
    'SEARCH_CONFIG',
    'securityService',
    'AlertService',
    function ($scope, $location, SearchResultsService, SEARCH_CONFIG, securityService, AlertService) {
        $scope.SearchResultsService = SearchResultsService;
        $scope.results = {};
        $scope.selectedSolution = SearchResultsService.currentSelection;
        $scope.searchInProgress = SearchResultsService.searchInProgress;
        $scope.currentSearchData = SearchResultsService.currentSearchData;
        $scope.itemsPerPage = 3;
        $scope.maxPagerSize = 5;
        $scope.currentPage = 1;
        $scope.selectPage = function (pageNum) {

            var start = $scope.itemsPerPage * (pageNum - 1);
            var end = start + $scope.itemsPerPage;
            end = end > SearchResultsService.results.length ? SearchResultsService.results.length : end;
            $scope.results = SearchResultsService.results.slice(start, end);
        };
        $scope.triggerAnalytics = function ($event) {
            if (this.isopen && window.chrometwo_require !== undefined && $location.path() === '/case/new') {
                chrometwo_require(['analytics/main'], function (analytics) {
                    analytics.trigger('OpenSupportCaseRecommendationClick', $event);
                });
            }
        };
        $scope.$watch(function () {
            return SearchResultsService.currentSelection;
        }, function (newVal) {
            $scope.selectedSolution = newVal;
        });

        $scope.$watch(function () {
            return SearchResultsService.results;
        }, function () {
            $scope.currentPage = 1;
            $scope.selectPage($scope.currentPage);
        }, true);
    }
]);

'use strict';
angular.module('RedhatAccess.cases').controller('ProductSelect', [
    '$scope',
    'securityService',
    'SearchCaseService',
    'CaseService',
    'strataService',
    'AlertService',
    function ($scope, securityService, SearchCaseService, CaseService, strataService, AlertService) {
        $scope.securityService = securityService;
        $scope.SearchCaseService = SearchCaseService;
        $scope.CaseService = CaseService;
        $scope.productsLoading = true;
        strataService.products.list().then(function (products) {
            $scope.productsLoading = false;
            CaseService.products = products;
        }, function (error) {
            $scope.productsLoading = false;
            AlertService.addStrataErrorMessage(error);
        });
    }
]);
'use strict';
/*jshint camelcase: false*/
angular.module('RedhatAccess.cases').controller('RecommendationsSection', [
    'RecommendationsService',
    '$scope',
    'strataService',
    'CaseService',
    'AlertService',
    function (RecommendationsService, $scope, strataService, CaseService, AlertService) {
        $scope.RecommendationsService = RecommendationsService;
        $scope.currentRecPin = {};
        $scope.pinRecommendation = function (recommendation, $index, $event) {
            $scope.currentRecPin = recommendation;
            $scope.currentRecPin.pinning = true;
            var doPut = function (linked) {
                var recJSON = {
                    recommendations: {
                        recommendation: [{
                            linked: linked.toString(),
                            resourceId: recommendation.id,
                            resourceType: 'Solution'
                        }]
                    }
                };
                strataService.cases.put(CaseService.kase.case_number, recJSON).then(function (response) {
                    if (!$scope.currentRecPin.pinned) {
                        //not currently pinned, so add it to the pinned list
                        RecommendationsService.pinnedRecommendations.push($scope.currentRecPin);
                    } else {
                        //currently pinned, so remove from pinned list
                        angular.forEach(RecommendationsService.pinnedRecommendations, function (rec, index) {
                            if (rec.id === $scope.currentRecPin.id) {
                                RecommendationsService.pinnedRecommendations.splice(index, 1);
                            }
                        });
                        //add the de-pinned rec to the top of the list
                        //this allows the user to still view the rec, or re-pin it
                        RecommendationsService.recommendations.splice(0, 0, $scope.currentRecPin);
                    }
                    $scope.currentRecPin.pinning = false;
                    $scope.currentRecPin.pinned = !$scope.currentRecPin.pinned;
                    RecommendationsService.selectPage(1);
                }, function (error) {
                    $scope.currentRecPin.pinning = false;
                    AlertService.addStrataErrorMessage(error);
                });
            };
            if (recommendation.pinned) {
                doPut(false);
            } else {
                doPut(true);
            }
        };
        $scope.triggerAnalytics = function ($event) {
            if (window.chrometwo_require !== undefined) {
                chrometwo_require(['analytics/main'], function (analytics) {
                    analytics.trigger('CaseViewRecommendationClick', $event);
                });
            }
        };
    }
]);

'use strict';
/*global $ */
/*jshint camelcase: false*/
angular.module('RedhatAccess.cases').controller('RequestManagementEscalationModal', [
    '$scope',
    '$modalInstance',
    'AlertService',
    'CaseService',
    'strataService',
    '$q',
    '$stateParams',
    'RHAUtils',
    function ($scope, $modalInstance, AlertService, CaseService, strataService, $q, $stateParams, RHAUtils) {
        $scope.CaseService = CaseService;
        $scope.submittingRequest = false;
        $scope.disableSubmitRequest = true;
        $scope.submitRequestClick = angular.bind($scope, function (commentText) {
            $scope.submittingRequest = true;
            var promises = [];
            var fullComment = 'Request Management Escalation: ' + commentText;
            var postComment;
            if (CaseService.draftComment) {
                postComment = strataService.cases.comments.put(CaseService.kase.case_number, fullComment, false, CaseService.draftComment.id);
            } else {
                postComment = strataService.cases.comments.post(CaseService.kase.case_number, fullComment, true, false);
            }
            postComment.then(function (response) {
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            promises.push(postComment);
            var caseJSON = { 'escalated': true };
            var updateCase = strataService.cases.put(CaseService.kase.case_number, caseJSON);
            updateCase.then(function (response) {
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            promises.push(updateCase);
            var masterPromise = $q.all(promises);
            masterPromise.then(function (response) {
                CaseService.populateComments($stateParams.id).then(function (comments) {
                    $scope.closeModal();
                    $scope.submittingRequest = false;
                });
            }, function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            return masterPromise;
        });
        $scope.closeModal = function () {
            CaseService.escalationCommentText = undefined;
            $modalInstance.close();
        };
        $scope.onNewEscalationComment = function () {
            if (RHAUtils.isNotEmpty(CaseService.escalationCommentText) && !$scope.submittingRequest) {
                $scope.disableSubmitRequest = false;
            } else if (RHAUtils.isEmpty(CaseService.escalationCommentText)) {
                $scope.disableSubmitRequest = true;
            }
        };
    }
]);

'use strict';
angular.module('RedhatAccess.cases').controller('Search', [
    '$scope',
    '$rootScope',
    'AUTH_EVENTS',
    'securityService',
    'SearchCaseService',
    'CaseService',
    'STATUS',
    'SearchBoxService',
    'AlertService',
    function ($scope, $rootScope, AUTH_EVENTS, securityService, SearchCaseService, CaseService, STATUS, SearchBoxService, AlertService) {
        $scope.securityService = securityService;
        $scope.SearchCaseService = SearchCaseService;
        $scope.CaseService = CaseService;
        $scope.itemsPerPage = 10;
        $scope.maxPagerSize = 5;
        $scope.selectPage = function (pageNum) {
            if (!SearchCaseService.allCasesDownloaded && $scope.itemsPerPage * pageNum / SearchCaseService.total >= 0.8) {
                SearchCaseService.doFilter().then(function () {
                    var start = $scope.itemsPerPage * (pageNum - 1);
                    var end = start + $scope.itemsPerPage;
                    end = end > SearchCaseService.cases.length ? SearchCaseService.cases.length : end;
                    $scope.casesOnScreen = SearchCaseService.cases.slice(start, end);
                });
            } else {
                var start = $scope.itemsPerPage * (pageNum - 1);
                var end = start + $scope.itemsPerPage;
                end = end > SearchCaseService.cases.length ? SearchCaseService.cases.length : end;
                $scope.casesOnScreen = SearchCaseService.cases.slice(start, end);
            }
        };
        SearchBoxService.doSearch = CaseService.onSelectChanged = CaseService.onOwnerSelectChanged = CaseService.onGroupSelectChanged = function () {
            SearchCaseService.clearPagination();
            SearchCaseService.doFilter().then(function () {
                $scope.selectPage(1);
            });
        };
        if (securityService.loginStatus.isLoggedIn) {
            CaseService.clearCase();
            SearchCaseService.clear();
            SearchBoxService.doSearch();
        }
        $scope.authEventLoginSuccess = $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
            SearchBoxService.doSearch();
            AlertService.clearAlerts();
        });
        $scope.authEventLogoutSuccess = $rootScope.$on(AUTH_EVENTS.logoutSuccess, function () {
            CaseService.clearCase();
            SearchCaseService.clear();
        });

        $scope.$on('$destroy', function () {
            $scope.authEventLoginSuccess();
            $scope.authEventLogoutSuccess();
        });
    }
]);

'use strict';
angular.module('RedhatAccess.cases').controller('SearchBox', [
    '$scope',
    'SearchBoxService',
    'securityService',
    function ($scope, SearchBoxService, securityService) {
        $scope.securityService = securityService;
        $scope.SearchBoxService = SearchBoxService;
        $scope.onFilterKeyPress = function ($event) {
            if ($event.keyCode === 13) {
                SearchBoxService.doSearch();
            } else if (angular.isFunction(SearchBoxService.onKeyPress)) {
                SearchBoxService.onKeyPress();
            }
        };
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('SeveritySelect', [
    '$scope',
    'securityService',
    'strataService',
    'CaseService',
    'AlertService',
    function ($scope, securityService, strataService, CaseService, AlertService) {
        $scope.securityService = securityService;
        $scope.CaseService = CaseService;
        $scope.severitiesLoading = true;
        strataService.values.cases.severity().then(function (severities) {
            $scope.severitiesLoading = false;
            CaseService.severities = severities;
        }, function (error) {
            $scope.severitiesLoading = false;
            AlertService.addStrataErrorMessage(error);
        });
    }
]);
'use strict';
angular.module('RedhatAccess.cases').controller('StatusSelect', [
    '$scope',
    'securityService',
    'CaseService',
    'STATUS',
    function ($scope, securityService, CaseService, STATUS) {
        $scope.securityService = securityService;
        $scope.CaseService = CaseService;
        $scope.STATUS = STATUS;
        $scope.statuses = [
            {
                name: 'Open and Closed',
                value: STATUS.both
            },
            {
                name: 'Open',
                value: STATUS.open
            },
            {
                name: 'Closed',
                value: STATUS.closed
            }
        ];
    }
]);

'use strict';
angular.module('RedhatAccess.cases').controller('TypeSelect', [
    '$scope',
    'securityService',
    'CaseService',
    'strataService',
    'AlertService',
    function ($scope, securityService, CaseService, strataService, AlertService) {
        $scope.securityService = securityService;
        $scope.CaseService = CaseService;
        $scope.typesLoading = true;
        strataService.values.cases.types().then(function (types) {
            $scope.typesLoading = false;
            CaseService.types = types;
        }, function (error) {
            $scope.typesLoading = false;
            AlertService.addStrataErrorMessage(error);
        });
    }
]);
'use strict';
angular.module('RedhatAccess.cases').directive('rhaAccountselect', function () {
    return {
        templateUrl: 'cases/views/accountSelect.html',
        restrict: 'A',
        controller: 'AccountSelect'
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaAddcommentsection', function () {
    return {
        templateUrl: 'cases/views/addCommentSection.html',
        restrict: 'A',
        controller: 'AddCommentSection'
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaAttachlocalfile', function () {
    return {
        templateUrl: 'cases/views/attachLocalFile.html',
        restrict: 'A',
        controller: 'AttachLocalFile',
        scope: { disabled: '=' }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaAttachproductlogs', function () {
    return {
        templateUrl: 'cases/views/attachProductLogs.html',
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCaseattachments', function () {
    return {
        templateUrl: 'cases/views/attachmentsSection.html',
        restrict: 'A',
        controller: 'AttachmentsSection',
        scope: { loading: '=' },
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaChatbutton', function () {
    return {
        scope: {},
        templateUrl: 'cases/views/chatButton.html',
        restrict: 'A',
        controller: 'ChatButton',
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});

'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCasecomments', ['$location','$anchorScroll' ,function ($location, $anchorScroll) {
    return {
        templateUrl: 'cases/views/commentsSection.html',
        controller: 'CommentsSection',
        scope: { loading: '=' },
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
            scope.commentReply = function(id) {
                var text = $('#'+id+' .pcmTextBlock').text();
                var person = $('#'+id+' .personNameBlock').text();
                var originalText = $('#case-comment-box').val();
                var lines = text.split(/\n/);
                text = '(In reply to ' + person + ')\n';
                for (var i = 0, max = lines.length; i < max; i++) {
                    text = text + '> '+ lines[i] + '\n';
                }
                if (originalText.trim() !== '') {
                    text = '\n' + text;
                }
                $('#case-comment-box').val($('#case-comment-box').val()+text).keyup();
                
                //Copying the code from the link to comment method
                var old = $location.hash();
                $location.hash('case-comment-box');
                $anchorScroll();
                $location.hash(old);
                $location.search('commentBox', 'commentBox');
            };
        }
    };
}]);

'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCompactcaselist', function () {
    return {
        templateUrl: 'cases/views/compactCaseList.html',
        controller: 'CompactCaseList',
        scope: {},
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCreategroupbutton', function () {
    return {
        templateUrl: 'cases/views/createGroupButton.html',
        restrict: 'A',
        controller: 'CreateGroupButton'
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaDeletegroupbutton', function () {
    return {
        templateUrl: 'cases/views/deleteGroupButton.html',
        restrict: 'A',
        controller: 'DeleteGroupButton'
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCasedescription', function () {
    return {
        templateUrl: 'cases/views/descriptionSection.html',
        restrict: 'A',
        scope: { loading: '=' },
        controller: 'DescriptionSection',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCasedetails', function () {
    return {
        templateUrl: 'cases/views/detailsSection.html',
        controller: 'DetailsSection',
        scope: {
            compact: '=',
            loading: '='
        },
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});
/*global angular*/
'use strict';
angular.module('RedhatAccess.cases').directive('rhaEmailnotifyselect', function () {
    return {
        templateUrl: 'cases/views/emailNotifySelect.html',
        restrict: 'A',
        transclude: true,
        controller: 'EmailNotifySelect',
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});

'use strict';
angular.module('RedhatAccess.cases').directive('rhaEntitlementselect', function () {
    return {
        templateUrl: 'cases/views/entitlementSelect.html',
        restrict: 'A',
        controller: 'EntitlementSelect'
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaExportcsvbutton', function () {
    return {
        templateUrl: 'cases/views/exportCSVButton.html',
        restrict: 'A',
        controller: 'ExportCSVButton'
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaGrouplist', function () {
    return {
        templateUrl: 'cases/views/groupList.html',
        restrict: 'A',
        controller: 'GroupList',
        link: function postLink(scope, element, attrs) {
	        scope.$on('$destroy', function () {
	            element.remove();
	        });
	    }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaGroupselect', function () {
    return {
        templateUrl: 'cases/views/groupSelect.html',
        restrict: 'A',
        controller: 'GroupSelect',
        scope: {
            onchange: '&'
        }
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaListattachments', function () {
    return {
        templateUrl: 'cases/views/listAttachments.html',
        restrict: 'A',
        controller: 'ListAttachments',
        scope: { disabled: '=' }
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaListbugzillas', function () {
    return {
        templateUrl: 'cases/views/listBugzillas.html',
        restrict: 'A',
        controller: 'ListBugzillas',
        scope: { loading: '=' },
        link: function postLink(scope, element, attrs) {
        }
    };
});
/*global angular*/
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaListfilter', function () {
    return {
        templateUrl: 'cases/views/listFilter.html',
        restrict: 'A',
        controller: 'ListFilter',
        scope: {
            prefilter: '=',
            postfilter: '='
        },
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaListnewattachments', function () {
    return {
        templateUrl: 'cases/views/listNewAttachments.html',
        restrict: 'A',
        controller: 'ListNewAttachments'
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaNewrecommendations', function () {
    return {
        templateUrl: 'cases/views/newRecommendationsSection.html',
        restrict: 'A',
        controller: 'PcmRecommendationsController',
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaOnchange', function () {
    return {
        restrict: 'A',
        link: function (scope, element, attrs) {
            element.bind('change', element.scope()[attrs.rhaOnchange]);
        }
    };
});
'use strict';
/*global angular*/
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaOwnerselect', function () {
    return {
        templateUrl: 'cases/views/ownerSelect.html',
        restrict: 'A',
        controller: 'OwnerSelect',
        scope: { onchange: '&' },
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaPageheader', function () {
    return {
        templateUrl: 'cases/views/pageHeader.html',
        restrict: 'A',
        scope: { title: '=title' },
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaProductselect', function () {
    return {
        templateUrl: 'cases/views/productSelect.html',
        restrict: 'A',
        controller: 'ProductSelect',
        scope: { onchange: '&' }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaCaserecommendations', function () {
    return {
        templateUrl: 'cases/views/recommendationsSection.html',
        restrict: 'A',
        controller: 'RecommendationsSection',
        transclude: true,
        scope: { loading: '=' },
        link: function postLink(scope, element, attrs) {
            scope.$on('$destroy', function () {
                element.remove();
            });
        }
    };
});

'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaSearchbox', function () {
    return {
        templateUrl: 'cases/views/searchBox.html',
        restrict: 'A',
        controller: 'SearchBox',
        scope: { placeholder: '=' }
    };
});
'use strict';
angular.module('RedhatAccess.cases').directive('rhaCasesearchresult', function () {
    return {
        templateUrl: 'cases/views/searchResult.html',
        restrict: 'A',
        scope: { theCase: '=case' }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaSelectloadingindicator', function () {
    return {
        templateUrl: 'cases/views/selectLoadingIndicator.html',
        restrict: 'A',
        transclude: true,
        scope: {
            loading: '=',
            type: '@'
        }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaSeverityselect', function () {
    return {
        templateUrl: 'cases/views/severitySelect.html',
        restrict: 'A',
        controller: 'SeveritySelect',
        scope: { onchange: '&' }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaStatusselect', function () {
    return {
        templateUrl: 'cases/views/statusSelect.html',
        restrict: 'A',
        controller: 'StatusSelect',
        scope: { onchange: '&' }
    };
});
'use strict';
/*jshint unused:vars */
angular.module('RedhatAccess.cases').directive('rhaTypeselect', function () {
    return {
        templateUrl: 'cases/views/typeSelect.html',
        restrict: 'A',
        controller: 'TypeSelect',
        scope: { onchange: '&' }
    };
});
'use strict';
angular.module('RedhatAccess.cases').filter('bytes', function () {
    return function (bytes, precision) {
        if (isNaN(parseFloat(bytes)) || !isFinite(bytes)) {
            return '-';
        }
        if (typeof precision === 'undefined') {
            precision = 1;
        }
        var units = [
                'bytes',
                'kB',
                'MB',
                'GB',
                'TB',
                'PB'
            ], number = Math.floor(Math.log(bytes) / Math.log(1024));
        return (bytes / Math.pow(1024, Math.floor(number))).toFixed(precision) + ' ' + units[number];
    };
});
'use strict';
angular.module('RedhatAccess.cases').filter('recommendationsResolution', function () {
    return function (text) {
        var shortText = '';
        var maxTextLength = 150;
        if (text !== undefined && text.length > maxTextLength) {
            shortText = text.substr(0, maxTextLength);
            // var lastSpace = shortText.lastIndexOf(' ');
            // shortText = shortText.substr(0, lastSpace);
            shortText = shortText.concat('...');
        } else {
            shortText = text;
        }
        return shortText;
    };
});
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').service('AttachmentsService', [
    '$filter',
    '$q',
    'strataService',
    'TreeViewSelectorUtils',
    '$http',
    'securityService',
    'AlertService',
    'CaseService',
    'translate',
    function ($filter, $q, strataService, TreeViewSelectorUtils, $http, securityService, AlertService, CaseService, translate) {
        this.originalAttachments = [];
        this.updatedAttachments = [];
        this.backendAttachments = [];
        this.suggestedArtifact = {};
        this.clear = function () {
            this.originalAttachments = [];
            this.updatedAttachments = [];
            this.backendAttachments = [];
        };
        this.updateBackEndAttachments = function (selected) {
            this.backendAttachments = selected;
        };
        this.hasBackEndSelections = function () {
            return TreeViewSelectorUtils.hasSelections(this.backendAttachments);
        };
        this.removeUpdatedAttachment = function ($index) {
            this.updatedAttachments.splice($index, 1);
        };
        this.removeOriginalAttachment = function ($index) {
            var attachment = this.originalAttachments[$index];
            var progressMessage = AlertService.addWarningMessage(translate('Deleting attachment:') + ' ' + attachment.file_name);
            strataService.cases.attachments.remove(attachment.uuid, CaseService.kase.case_number).then(angular.bind(this, function () {
                AlertService.removeAlert(progressMessage);
                AlertService.addSuccessMessage(translate('Successfully deleted attachment:') + ' ' + attachment.file_name);
                this.originalAttachments.splice($index, 1);
            }), function (error) {
                AlertService.addStrataErrorMessage(error);
            });
        };
        this.addNewAttachment = function (attachment) {
            this.updatedAttachments.push(attachment);
        };
        this.defineOriginalAttachments = function (attachments) {
            if (!angular.isArray(attachments)) {
                this.originalAttachments = [];
            } else {
                this.originalAttachments = attachments;
            }
        };
        this.postBackEndAttachments = function (caseId) {
            var selectedFiles = TreeViewSelectorUtils.getSelectedLeaves(this.backendAttachments);
            return securityService.getBasicAuthToken().then(function (auth) {
                /*jshint unused:false */
                //we post each attachment separately
                var promises = [];
                angular.forEach(selectedFiles, function (file) {
                    var jsonData = {
                            authToken: auth,
                            attachment: file,
                            caseNum: caseId
                        };
                    var deferred = $q.defer();
                    $http.post('attachments', jsonData).success(function (data, status, headers, config) {
                        deferred.resolve(data);
                        AlertService.addSuccessMessage(translate('Successfully uploaded attachment') + ' ' + jsonData.attachment + ' ' + translate('to case') + ' ' + caseId);
                    }).error(function (data, status, headers, config) {
                        var errorMsg = '';
                        switch (status) {
                        case 401:
                            errorMsg = ' : Unauthorised.';
                            break;
                        case 409:
                            errorMsg = ' : Invalid username/password.';
                            break;
                        case 500:
                            errorMsg = ' : Internal server error';
                            break;
                        }
                        AlertService.addDangerMessage('Failed to upload attachment ' + jsonData.attachment + ' to case ' + caseId + errorMsg);
                        deferred.reject(data);
                    });
                    promises.push(deferred.promise);
                });
                return $q.all(promises);
            });
        };
        this.updateAttachments = function (caseId) {
            var hasServerAttachments = this.hasBackEndSelections();
            var hasLocalAttachments = !angular.equals(this.updatedAttachments.length, 0);
            if (hasLocalAttachments || hasServerAttachments) {
                var promises = [];
                var updatedAttachments = this.updatedAttachments;
                if (hasServerAttachments) {
                    promises.push(this.postBackEndAttachments(caseId));
                }
                if (hasLocalAttachments) {
                    //find new attachments
                    angular.forEach(updatedAttachments, function (attachment) {
                        if (!attachment.hasOwnProperty('uuid')) {
                            var promise = strataService.cases.attachments.post(attachment.file, caseId);
                            promise.then(function (uri) {
                                attachment.uri = uri;
                                attachment.uuid = uri.slice(uri.lastIndexOf('/') + 1);
                                AlertService.addSuccessMessage('Successfully uploaded attachment ' + attachment.file_name + ' to case ' + caseId);
                            }, function (error) {
                                AlertService.addStrataErrorMessage(error);
                            });
                            promises.push(promise);
                        }
                    });
                }
                var uploadingAlert = AlertService.addWarningMessage('Uploading attachments...');
                var parentPromise = $q.all(promises);
                parentPromise.then(angular.bind(this, function () {
                    this.originalAttachments = this.originalAttachments.concat(this.updatedAttachments);
                    this.updatedAttachments = [];
                    AlertService.removeAlert(uploadingAlert);
                }), function (error) {
                    AlertService.addStrataErrorMessage(error);
                    AlertService.removeAlert(uploadingAlert);
                });
                return parentPromise;
            }
        };
    }
]);
'use strict';
angular.module('RedhatAccess.cases').service('CaseListService', [function () {
        this.cases = [];
        this.defineCases = function (cases) {
            this.cases = cases;
        };
    }]);
'use strict';
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').constant('CASE_GROUPS', {
    manage: 'manage',
    ungrouped: 'ungrouped'
}).service('CaseService', [
    'strataService',
    'AlertService',
    'RHAUtils',
    'securityService',
    '$q',
    '$timeout',
    '$filter',
    function (strataService, AlertService, RHAUtils, securityService, $q, $timeout, $filter) {
        this.kase = {};
        this.caseDataReady = false;
        this.isCommentPublic = false;
        this.versions = [];
        this.products = [];
        //this.statuses = [];
        this.severities = [];
        this.groups = [];
        this.users = [];
        this.comments = [];
        this.originalNotifiedUsers = [];
        this.updatedNotifiedUsers = [];
        this.account = {};
        this.draftComment = {};
        this.commentText = '';
        this.escalationCommentText = '';
        this.status = '';
        this.severity = '';
        this.type = '';
        this.group = '';
        this.owner = '';
        this.product = '';
        this.bugzillaList = {};
        this.onSelectChanged = null;
        this.onOwnerSelectChanged = null;
        this.onGroupSelectChanged = null;
        this.groupOptions = [];
        this.showsearchoptions = false;
        this.disableAddComment = true;
        /**
       * Add the necessary wrapper objects needed to properly display the data.
       *
       * @param rawCase
       */
        this.defineCase = function (rawCase) {
            /*jshint camelcase: false */
            rawCase.severity = { 'name': rawCase.severity };
            rawCase.status = { 'name': rawCase.status };
            rawCase.product = { 'name': rawCase.product };
            rawCase.group = { 'number': rawCase.folder_number };
            rawCase.type = { 'name': rawCase.type };
            this.kase = rawCase;
            this.bugzillaList = rawCase.bugzillas;
            this.caseDataReady = true;
        };
        this.setCase = function (jsonCase) {
            this.kase = jsonCase;
            this.bugzillaList = jsonCase.bugzillas;
            this.caseDataReady = true;
        };
        this.defineAccount = function (account) {
            this.account = account;
        };
        this.defineNotifiedUsers = function () {
            /*jshint camelcase: false */
            this.updatedNotifiedUsers.push(this.kase.contact_sso_username);
            //hide the X button for the case owner
            $('#rha-emailnotifyselect').on('change', angular.bind(this, function () {
                $('rha-emailnotifyselect .chosen-choices li:contains("' + this.kase.contact_sso_username + '") a').css('display', 'none');
                $('rha-emailnotifyselect .chosen-choices li:contains("' + this.kase.contact_sso_username + '")').css('padding-left', '5px');
            }));
            if (RHAUtils.isNotEmpty(this.kase.notified_users)) {
                angular.forEach(this.kase.notified_users.link, angular.bind(this, function (user) {
                    this.originalNotifiedUsers.push(user.sso_username);
                }));
                this.updatedNotifiedUsers = this.updatedNotifiedUsers.concat(this.originalNotifiedUsers);
            }
        };
        this.getGroups = function () {
            return this.groups;
        };
        this.clearCase = function () {
            this.caseDataReady = false;
            this.isCommentPublic = false;
            this.kase = {};
            this.versions = [];
            this.products = [];
            this.statuses = [];
            this.severities = [];
            this.groups = [];
            this.account = {};
            this.comments = [];
            this.bugzillaList = {};
            this.draftComment = undefined;
            this.commentText = undefined;
            this.escalationCommentText = undefined;
            this.status = undefined;
            this.severity = undefined;
            this.type = undefined;
            this.group = undefined;
            this.owner = undefined;
            this.product = undefined;
            this.originalNotifiedUsers = [];
            this.updatedNotifiedUsers = [];
            this.groupOptions = [];
        };
        this.groupsLoading = false;
        this.populateGroups = function (ssoUsername) {
            var that = this;
            var deferred = $q.defer();
            this.groupsLoading = true;
            var username = ssoUsername;
            if(username === undefined){
                username = securityService.loginStatus.authedUser.sso_username;
            }
            strataService.groups.list(ssoUsername).then(angular.bind(this, function (groups) {
                that.groups = groups;
                that.group = '';
                that.buildGroupOptions(that);
                that.groupsLoading = false;
                deferred.resolve(groups);
            }), angular.bind(this, function (error) {
                that.groupsLoading = false;
                AlertService.addStrataErrorMessage(error);
                deferred.reject();
            }));
            return deferred.promise;
        };
        this.usersLoading = false;
        /**
       *  Intended to be called only after user is logged in and has account details
       *  See securityService.
       */
        this.populateUsers = angular.bind(this, function () {
            var promise = null;
            if (securityService.loginStatus.authedUser.org_admin) {
                this.usersLoading = true;
                var accountNumber = RHAUtils.isEmpty(this.account.number) ? securityService.loginStatus.authedUser.account_number : this.account.number;
                promise = strataService.accounts.users(accountNumber);
                promise.then(angular.bind(this, function (users) {
                    angular.forEach(users, function(user){
                        if(user.sso_username === securityService.loginStatus.authedUser.sso_username) {
                            this.owner = user.sso_username;
                        }
                    }, this);
                    this.usersLoading = false;
                    this.users = users;
                }), angular.bind(this, function (error) {
                    this.users = [];
                    this.usersLoading = false;
                    AlertService.addStrataErrorMessage(error);
                }));
            } else {
                var deferred = $q.defer();
                promise = deferred.promise;
                deferred.resolve();
                var tmp= {'sso_username': securityService.loginStatus.authedUser.sso_username};
                this.users.push(tmp);
            }
            return promise;
        });

        this.scrollToComment = function(commentID) {
            if(!commentID) {
                return;
            }
            var commentElem = document.getElementById(commentID);
            if(commentElem) {
                commentElem.scrollIntoView(true);
            }
        };
        this.populateComments = function (caseNumber) {
            var promise = strataService.cases.comments.get(caseNumber);
            promise.then(angular.bind(this, function (comments) {
                //pull out the draft comment
                angular.forEach(comments, angular.bind(this, function (comment, index) {
                    if (comment.draft === true) {
                        this.draftComment = comment;
                        this.commentText = comment.text;
                        this.isCommentPublic = comment.public;
                        if (RHAUtils.isNotEmpty(this.commentText)) {
                            this.disableAddComment = false;
                        } else if (RHAUtils.isEmpty(this.commentText)) {
                            this.disableAddComment = true;
                        }
                        comments.slice(index, index + 1);
                    }
                }));
                this.comments = comments;
            }), function (error) {
                AlertService.addStrataErrorMessage(error);
            });
            return promise;
        };
        this.entitlementsLoading = false;
        this.populateEntitlements = function (ssoUserName) {
            this.entitlementsLoading = true;
            strataService.entitlements.get(false, ssoUserName).then(angular.bind(this, function (entitlementsResponse) {
                // if the user has any premium or standard level entitlement, then allow them
                // to select it, regardless of the product.
                // TODO: strata should respond with a filtered list given a product.
                //       Adding the query param ?product=$PRODUCT does not work.
                var uniqueEntitlements = function (a) {
                    return a.reduce(function (p, c) {
                        if (p.indexOf(c.sla) < 0) {
                            p.push(c.sla);
                        }
                        return p;
                    }, []);
                };
                var entitlements = uniqueEntitlements(entitlementsResponse.entitlement);
                var unknownIndex = entitlements.indexOf('UNKNOWN');
                if (unknownIndex > -1) {
                    entitlements.splice(unknownIndex, 1);
                }
                this.entitlements = entitlements;
                this.entitlementsLoading = false;
            }), angular.bind(this, function (error) {
                AlertService.addStrataErrorMessage(error);
            }));
        };
        this.showFts = function () {
            if (RHAUtils.isNotEmpty(this.severities)) {
                if (this.entitlement === 'PREMIUM' || this.entitlement === 'AMC' || RHAUtils.isNotEmpty(this.kase.entitlement) && (this.kase.entitlement.sla === 'PREMIUM' || this.kase.entitlement.sla === 'AMC')) {
                    return true;
                }
            }
            return false;
        };
        this.newCasePage1Incomplete = true;
        this.validateNewCasePage1 = function () {
            if (RHAUtils.isEmpty(this.kase.product) || RHAUtils.isEmpty(this.kase.version) || RHAUtils.isEmpty(this.kase.summary) || RHAUtils.isEmpty(this.kase.description)) {
                this.newCasePage1Incomplete = true;
            } else {
                this.newCasePage1Incomplete = false;
            }
        };
        this.showVersionSunset = function () {
            if (RHAUtils.isNotEmpty(this.kase.product) && RHAUtils.isNotEmpty(this.kase.version)) {
                if ((this.kase.version).toLowerCase().indexOf('- eol') > -1) {
                    return true;
                }
            }
            return false;
        };

        this.buildGroupOptions = function() {
            this.groupOptions = [];
            var sep = '────────────────────────────────────────';
            this.groups.sort(function(a, b){
                if(a.name < b.name) { return -1; }
                if(a.name > b.name) { return 1; }
                return 0;
            });

            var defaultGroup = '';
            if (this.showsearchoptions === true) {
                this.groupOptions.push({
                    value: '',
                    label: 'All Groups'
                }, {
                    value: 'ungrouped',
                    label: 'Ungrouped Cases'
                }, {
                    isDisabled: true,
                    label: sep
                });
            } else {
                this.groupOptions.push({
                    value: '',
                    label: 'Ungrouped Case'
                });
            }

            angular.forEach(this.groups, function(group){
                this.groupOptions.push({
                    value: group.number,
                    label: group.name
                });
                if(group.is_default) {
                    this.kase.group = group.number;
                    this.group = group.number;
                }
            }, this);
            if (this.showsearchoptions === true) {
                this.groupOptions.push({
                    isDisabled: true,
                    label: sep
                }, {
                    value: 'manage',
                    label: 'Manage Case Groups'
                });
            }
        };
    }
]);

'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').service('GroupService', [
    'strataService',
    function (strataService) {
        this.reloadTable = {};
        this.groupsOnScreen = [];
        this.disableDeleteGroup = true;
    }
]);

'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').service('GroupUserService', [
    'strataService',
    function (strataService) {
        this.reloadTable = {};
        this.groupsOnScreen = [];
    }
]);
'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').service('RecommendationsService', [
    'strataService',
    'CaseService',
    'AlertService',
    '$q',
    function (strataService, CaseService, AlertService, $q) {
        this.recommendations = [];
        this.pinnedRecommendations = [];
        this.handPickedRecommendations = [];
        var currentData = {
                product: null,
                version: null,
                summary: null,
                description: null
            };
        this.loadingRecommendations = false;
        var setCurrentData = function () {
            currentData = {
                product: CaseService.kase.product,
                version: CaseService.kase.version,
                summary: CaseService.kase.summary,
                description: CaseService.kase.description
            };
        };
        setCurrentData();
        this.clear = function () {
            this.recommendations = [];
        };
        this.pageSize = 4;
        this.maxSize = 10;
        this.recommendationsOnScreen = [];
        this.selectPage = function (pageNum) {
            //filter out pinned recommendations
            angular.forEach(this.pinnedRecommendations, angular.bind(this, function (pinnedRec) {
                angular.forEach(this.recommendations, angular.bind(this, function (rec, index) {
                    if (angular.equals(rec.id, pinnedRec.id)) {
                        this.recommendations.splice(index, 1);
                    }
                }));
            }));
            angular.forEach(this.handPickedRecommendations, angular.bind(this, function (handPickedRec) {
                angular.forEach(this.recommendations, angular.bind(this, function (rec, index) {
                    if (angular.equals(rec.id, handPickedRec.id)) {
                        this.recommendations.splice(index, 1);
                    }
                }));
            }));
            var recommendations = this.pinnedRecommendations.concat(this.recommendations);
            recommendations = this.handPickedRecommendations.concat(recommendations);
            var start = this.pageSize * (pageNum - 1);
            var end = start + this.pageSize;
            end = end > recommendations.length ? recommendations.length : end;
            this.recommendationsOnScreen = recommendations.slice(start, end);
            this.currentPage = pageNum;
        };
        this.populatePinnedRecommendations = function () {
            var promises = [];
            if (CaseService.kase.recommendations) {
                //Push any pinned recommendations to the front of the array
                if (CaseService.kase.recommendations.recommendation) {
                    var promise = {};
                    angular.forEach(CaseService.kase.recommendations.recommendation, angular.bind(this, function (rec) {
                        if (rec.pinned_at) {
                            promise = strataService.solutions.get(rec.resource_id).then(angular.bind(this, function (solution) {
                                    solution.pinned = true;
                                    this.pinnedRecommendations.push(solution);
                                }), function (error) {
                                    AlertService.addStrataErrorMessage(error);
                                });
                            promises.push(promise);
                        } else if (rec.linked) {
                            promise = strataService.solutions.get(rec.resource_id).then(angular.bind(this, function (solution) {
                                    //solution.pinned = true;
                                    solution.handPicked = true;
                                    this.handPickedRecommendations.push(solution);
                                }), function (error) {
                                    AlertService.addStrataErrorMessage(error);
                                });
                            promises.push(promise);
                        }
                    }));
                }
            }
            var masterPromise = $q.all(promises);
            masterPromise.then(angular.bind(this, function () {
                this.selectPage(1);
            }));
            return masterPromise;
        };
        this.failureCount = 0;
        this.populateRecommendations = function (max) {
            var masterDeferred = $q.defer();
            masterDeferred.promise.then(angular.bind(this, function() {this.selectPage(1);}));
            var productName;
            if(CaseService.kase.product !== undefined && CaseService.kase.product.name !== undefined){
                productName = CaseService.kase.product.name;
            }
            var newData = {
                    product: productName,
                    version: CaseService.kase.version,
                    summary: CaseService.kase.summary,
                    description: CaseService.kase.description
                };
            if ((newData.product !== undefined || newData.version !== undefined || newData.summary !== undefined || newData.description !== undefined || (!angular.equals(currentData, newData) && !this.loadingRecommendations || this.recommendations.length < 1)) && this.failureCount < 10) {
                this.loadingRecommendations = true;
                setCurrentData();
                var deferreds = [];
                strataService.recommendations(currentData, max).then(angular.bind(this, function (solutions) {
                    //retrieve details for each solution
                    solutions.forEach(function (solution) {
                        var deferred = strataService.solutions.get(solution.resource_uri);
                        deferreds.push(deferred);
                    });
                    $q.all(deferreds).then(angular.bind(this, function (solutions) {
                        this.recommendations = [];
                        solutions.forEach(angular.bind(this, function (solution) {
                            if (solution !== undefined) {
                                solution.resource_type = 'Solution';
                                this.recommendations.push(solution);
                            }
                        }));
                        this.loadingRecommendations = false;
                        masterDeferred.resolve();
                    }), angular.bind(this, function (error) {
                        this.loadingRecommendations = false;
                        masterDeferred.resolve();
                    }));
                }), angular.bind(this, function (error) {
                    this.loadingRecommendations = false;
                    masterDeferred.reject();
                    this.failureCount++;
                    this.populateRecommendations(12);
                }));
            } else {
                masterDeferred.resolve();
            }
            return masterDeferred.promise;
        };
    }
]);

'use strict';
/*jshint unused:vars */
/*jshint camelcase: false */
angular.module('RedhatAccess.cases').service('SearchBoxService', [function () {
        this.doSearch = {};
        this.searchTerm = undefined;
        this.onKeyPress = {};
    }]);

/*jshint camelcase: false*/
'use strict';
angular.module('RedhatAccess.cases').service('SearchCaseService', [
    'CaseService',
    'strataService',
    'AlertService',
    'STATUS',
    'CASE_GROUPS',
    'AUTH_EVENTS',
    '$q',
    '$state',
    '$rootScope',
    'SearchBoxService',
    'securityService',
    function (CaseService, strataService, AlertService, STATUS, CASE_GROUPS, AUTH_EVENTS, $q, $state, $rootScope, SearchBoxService, securityService) {
        this.cases = [];
        this.totalCases = 0;
        this.searching = true;
        this.prefilter = {};
        this.postfilter = {};
        this.start = 0;
        this.count = 100;
        this.total = 0;
        this.allCasesDownloaded = false;
        var getIncludeClosed = function () {
            if (CaseService.status === STATUS.open) {
                return false;
            } else if (CaseService.status === STATUS.closed) {
                return true;
            } else if (CaseService.status === STATUS.both) {
                return true;
            }
            return true;
        };
        this.clear = function () {
            this.cases = [];
            this.oldParams = {};
            SearchBoxService.searchTerm = '';
            this.start = 0;
            this.total = 0;
            this.totalCases = 0;
            this.allCasesDownloaded = false;
            this.prefilter = {};
            this.postfilter = {};
            this.searching = true;
        };
        this.clearPagination = function () {
            this.start = 0;
            this.total = 0;
            this.allCasesDownloaded = false;
            this.cases = [];
        };
        this.oldParams = {};
        this.doFilter = function () {
            if (angular.isFunction(this.prefilter)) {
                this.prefilter();
            }
            if(this.start > 0){
                this.count = this.totalCases - this.start;
            }
            var params = {
                count: this.count,
                include_closed: getIncludeClosed(),
            };
            params.start = this.start;
            var isObjectNothing = function (object) {
                if (object === '' || object === undefined || object === null) {
                    return true;
                } else {
                    return false;
                }
            };
            if (!isObjectNothing(SearchBoxService.searchTerm)) {
                params.keyword = SearchBoxService.searchTerm;
            }
            if (CaseService.group === CASE_GROUPS.manage) {
                $state.go('group');
            } else if (CaseService.group === CASE_GROUPS.ungrouped) {
                params.only_ungrouped = true;
            } else if (!isObjectNothing(CaseService.group)) {
                params.group_numbers = { group_number: [CaseService.group] };
            }
            if (CaseService.status === STATUS.closed) {
                params.status = STATUS.closed;
            }
            if (!isObjectNothing(CaseService.product)) {
                params.product = CaseService.product;
            }
            if (!isObjectNothing(CaseService.owner)) {
                params.owner_ssoname = CaseService.owner;
            }
            if (!isObjectNothing(CaseService.type)) {
                params.type = CaseService.type;
            }
            if (!isObjectNothing(CaseService.severity)) {
                params.severity = CaseService.severity;
            }
            this.searching = true;
            //TODO: hack to get around onchange() firing at page load for each select.
            //Need to prevent initial onchange() event instead of handling here.
            var promises = [];
            var deferred = $q.defer();
            if (!angular.equals(params, this.oldParams)) {
                this.oldParams = params;
                var that = this;
                var cases = null;
                if (securityService.loginStatus.isLoggedIn) {
                    if (securityService.loginStatus.authedUser.sso_username && securityService.loginStatus.authedUser.is_internal) {
                        params.owner_ssoname = securityService.loginStatus.authedUser.sso_username;
                    }
                    cases = strataService.cases.filter(params).then(angular.bind(that, function (response) {
                        if(response['case'] === undefined ){
                            that.totalCases = 0;
                            that.total = 0;
                        } else {
                            that.totalCases = response.total_count;
                            if (response['case'] !== undefined && response['case'].length + that.total >= that.totalCases) {
                                that.allCasesDownloaded = true;
                            }
                            that.cases = that.cases.concat(response['case']);
                            that.start = that.start + that.count;
                            if (response['case'] !== undefined){
                                that.total = that.total + response['case'].length;
                            }
                            if (angular.isFunction(that.postFilter)) {
                                that.postFilter();
                            }
                        }
                        that.searching = false;
                    }), angular.bind(that, function (error) {
                        AlertService.addStrataErrorMessage(error);
                        that.searching = false;
                    }));
                    deferred.resolve(cases);
                } else {
                    $rootScope.$on(AUTH_EVENTS.loginSuccess, function () {
                        if (securityService.loginStatus.authedUser.sso_username && securityService.loginStatus.authedUser.is_internal) {
                            params.owner_ssoname = securityService.loginStatus.authedUser.sso_username;
                        }
                        cases = strataService.cases.filter(params).then(angular.bind(that, function (response) {
                            that.totalCases = response.total_count;
                            
                            that.cases = that.cases.concat(response['case']);
                            that.searching = false;
                            that.start = that.start + that.count;
                            that.total = that.total + response['case'].length;
                            if (that.total >= that.totalCases) {
                                that.allCasesDownloaded = true;
                            }
                            if (angular.isFunction(that.postFilter)) {
                                that.postFilter();
                            }
                        }), angular.bind(that, function (error) {
                            AlertService.addStrataErrorMessage(error);
                            that.searching = false;
                        }));
                        deferred.resolve(cases);
                    });
                }
                promises.push(deferred.promise);
            } else {
                deferred.resolve();
                promises.push(deferred.promise);
            }
            return $q.all(promises);
        };
    }
]);

'use strict';
angular.module('RedhatAccess.logViewer').controller('AccordionDemoCtrl', [
    '$scope',
    'accordian',
    function ($scope, accordian) {
        $scope.oneAtATime = true;
        $scope.groups = accordian.getGroups();
    }
]);
/*global parseList*/
'use strict';
angular.module('RedhatAccess.logViewer').controller('DropdownCtrl', [
    '$scope',
    '$http',
    '$location',
    'files',
    'hideMachinesDropdown',
    'AlertService',
    function ($scope, $http, $location, files, hideMachinesDropdown, AlertService) {
        $scope.machinesDropdownText = 'Please Select the Machine';
        $scope.items = [];
        $scope.hideDropdown = hideMachinesDropdown.value;
        $scope.loading = false;
        var sessionId = $location.search().sessionId;
        $scope.getMachines = function () {
            $http({
                method: 'GET',
                url: 'machines?sessionId=' + encodeURIComponent(sessionId)
            }).success(function (data, status, headers, config) {
                $scope.items = data;
            }).error(function (data, status, headers, config) {
                AlertService.addDangerMessage(data);
            });
        };
        $scope.machineSelected = function () {
            $scope.loading = true;
            var sessionId = $location.search().sessionId;
            var userId = $location.search().userId;
            files.selectedHost = this.choice;
            $scope.machinesDropdownText = this.choice;
            $http({
                method: 'GET',
                url: 'logs?machine=' + files.selectedHost + '&sessionId=' + encodeURIComponent(sessionId) + '&userId=' + encodeURIComponent(userId)
            }).success(function (data, status, headers, config) {
                $scope.loading = false;
                var tree = [];
                parseList(tree, data);
                files.setFileList(tree);
            }).error(function (data, status, headers, config) {
                $scope.loading = false;
                AlertService.addDangerMessage(data);
            });
        };
        if ($scope.hideDropdown) {
            $scope.machineSelected();
        } else {
            $scope.getMachines();
        }
    }
]);

'use strict';
angular.module('RedhatAccess.logViewer').controller('TabsDemoCtrl', [
    '$rootScope',
    '$scope',
    '$http',
    '$location',
    'files',
    'accordian',
    'SearchResultsService',
    'securityService',
    'AlertService',
    'LOGVIEWER_EVENTS',
    function ($rootScope, $scope, $http, $location, files, accordian, SearchResultsService, securityService, AlertService, LOGVIEWER_EVENTS) {
        $scope.tabs = [];
        $scope.isLoading = false;
        $scope.$watch(function () {
            return files.getFileClicked().check;
        }, function () {
            if (files.getFileClicked().check && files.selectedFile !== undefined) {
                var tab = {};
                if (files.selectedHost !== undefined) {
                    tab.longTitle = files.selectedHost + ':';
                } else {
                    tab.longTitle = '';
                }
                tab.longTitle = tab.longTitle.concat(files.selectedFile);
                var splitFileName = files.selectedFile.split('/');
                var fileName = splitFileName[splitFileName.length - 1];
                if (files.selectedHost !== undefined) {
                    tab.shortTitle = files.selectedHost + ':';
                } else {
                    tab.shortTitle = '';
                }
                tab.shortTitle = tab.shortTitle.concat(fileName);
                tab.active = true;
                $scope.tabs.push(tab);
                $scope.isLoading = true;
                files.setActiveTab(tab);
                files.setFileClicked(false);
            }
        });
        $scope.$watch(function () {
            return files.file;
        }, function () {
            if (files.file && files.activeTab) {
                files.activeTab.content = files.file;
                $scope.isLoading = false;
                files.file = undefined;
            }
        });
        $scope.$watch(function () {
            return SearchResultsService.searchInProgress.value;
        }, function () {
            if (SearchResultsService.searchInProgress.value === true) {
                $scope.$parent.isDisabled = true;
            } else if (SearchResultsService.searchInProgress.value === false && $scope.$parent.textSelected === true) {
                $scope.$parent.isDisabled = false;
            }
        });
        $scope.removeTab = function (index) {
            $scope.tabs.splice(index, 1);
            if ($scope.tabs.length < 1) {
                $rootScope.$broadcast(LOGVIEWER_EVENTS.allTabsClosed);
            }
        };
        $scope.checked = false;
        // This will be
        // binded using the
        // ps-open attribute
        $scope.diagnoseText = function () {
            //$scope.isDisabled = true;
            var text = strata.utils.getSelectedText();
            securityService.validateLogin(true).then(function () {
                //Removed in refactor, no loger exists.  Think it hides tool tip??
                //this.tt_isOpen = false;
                if (!$scope.$parent.solutionsToggle) {
                    $scope.$parent.solutionsToggle = !$scope.$parent.solutionsToggle;
                }
                if (text !== '') {
                    $scope.checked = !$scope.checked;
                    SearchResultsService.diagnose(text, 5);
                }
            });    // this.tt_isOpen = false;
                   // if (!$scope.$parent.solutionsToggle) {
                   // 	$scope.$parent.solutionsToggle = !$scope.$parent.solutionsToggle;
                   // }
                   // var text = strata.utils.getSelectedText();
                   // if (text != "") {
                   // 	$scope.checked = !$scope.checked;
                   // 	SearchResultsService.diagnose(text, 5);
                   // }
                   //$scope.sleep(5000, $scope.checkTextSelection);
        };
        $scope.refreshTab = function (index) {
            var sessionId = $location.search().sessionId;
            var userId = $location.search().userId;
            var fileNameForRefresh = this.$parent.tab.longTitle;
            var hostForRefresh = null;
            var splitNameForRefresh = fileNameForRefresh.split(':');
            if (splitNameForRefresh[0] && splitNameForRefresh[1]) {
                $scope.isLoading = true;
                hostForRefresh = splitNameForRefresh[0];
                fileNameForRefresh = splitNameForRefresh[1];
                $http({
                    method: 'GET',
                    url: 'logs?sessionId=' + encodeURIComponent(sessionId) + '&userId=' + encodeURIComponent(userId) + '&path=' + fileNameForRefresh + '&machine=' + hostForRefresh
                }).success(function (data, status, headers, config) {
                    $scope.isLoading = false;
                    $scope.tabs[index].content = data;
                }).error(function (data, status, headers, config) {
                    $scope.isLoading = false;
                    AlertService.addDangerMessage(data);
                });
            }
        };
    }
]);
'use strict';
angular.module('RedhatAccess.logViewer').controller('fileController', [
    '$scope',
    '$rootScope',
    '$http',
    '$location',
    'files',
    'AlertService',
    'LOGVIEWER_EVENTS',
    function ($scope, $rootScope, $http, $location, files, AlertService, LOGVIEWER_EVENTS) {
        $scope.roleList = '';
        $scope.retrieveFileButtonIsDisabled = files.getRetrieveFileButtonIsDisabled();
        $scope.$watch(function () {
            return $scope.mytree.currentNode;
        }, function () {
            if ($scope.mytree.currentNode !== undefined && $scope.mytree.currentNode.fullPath !== undefined) {
                files.setSelectedFile($scope.mytree.currentNode.fullPath);
                files.setRetrieveFileButtonIsDisabled(false);
            } else {
                files.setRetrieveFileButtonIsDisabled(true);
            }
        });
        $scope.$watch(function () {
            return files.fileList;
        }, function () {
            $scope.roleList = files.fileList;
        });

        $scope.selectItem = function(){
            if(files.selectedFile !== undefined && !files.getRetrieveFileButtonIsDisabled()){
                $scope.fileSelected();
            }
        };

        $scope.fileSelected = function () {
            files.setFileClicked(true);
            var sessionId = $location.search().sessionId;
            var userId = $location.search().userId;
            $scope.$parent.$parent.sidePaneToggle = !$scope.$parent.$parent.sidePaneToggle;
            $http({
                method: 'GET',
                url: 'logs?sessionId=' + encodeURIComponent(sessionId) + '&userId=' + encodeURIComponent(userId) + '&path=' + files.selectedFile + '&machine=' + files.selectedHost
            }).success(function (data, status, headers, config) {
                files.file = data;
            }).error(function (data, status, headers, config) {
                AlertService.addDangerMessage(data);
            });
        };
        $rootScope.$on(LOGVIEWER_EVENTS.allTabsClosed, function () {
            $scope.$parent.$parent.sidePaneToggle = !$scope.$parent.$parent.sidePaneToggle;
        });
    }
]);

'use strict';
angular.module('RedhatAccess.logViewer').controller('logViewerController', [
    '$scope',
    'SearchResultsService',
    function ($scope, SearchResultsService) {
        $scope.isDisabled = true;
        $scope.textSelected = false;
        $scope.showSolutions = false;
        $scope.enableDiagnoseButton = function () {
            //Gotta wait for text to "unselect"
            $scope.sleep(1, $scope.checkTextSelection);
        };
        $scope.checkTextSelection = function () {
            if (strata.utils.getSelectedText()) {
                $scope.textSelected = true;
                if (SearchResultsService.searchInProgress.value) {
                    $scope.isDisabled = true;
                } else {
                    $scope.isDisabled = false;
                }
            } else {
                $scope.textSelected = false;
                $scope.isDisabled = true;
            }
            $scope.$apply();
        };
        $scope.sleep = function (millis, callback) {
            setTimeout(function () {
                callback();
            }, millis);
        };
        $scope.toggleSolutions = function () {
            $scope.showSolutions = !$scope.showSolutions;
        };
    }
]);
'use strict';
angular.module('RedhatAccess.logViewer').directive('rhaFilldown', [
    '$window',
    '$timeout',
    function ($window, $timeout) {
        return {
            restrict: 'A',
            link: function postLink(scope, element) {
                scope.onResizeFunction = function () {
                    var distanceToTop = element[0].getBoundingClientRect().top;
                    var height = $window.innerHeight - distanceToTop - 21;
                    if (element[0].id === 'fileList') {
                        height -= 34;
                    }
                    scope.windowHeight = height;
                    return scope.windowHeight;
                };
                // This might be overkill??
                //scope.onResizeFunction();
                angular.element($window).bind('resize', function () {
                    scope.onResizeFunction();
                    scope.$apply();
                });
                angular.element($window).bind('click', function () {
                    scope.onResizeFunction();
                    scope.$apply();
                });
                $timeout(scope.onResizeFunction, 100);    // $(window).load(function(){
                                                          //  scope.onResizeFunction();
                                                          //  scope.$apply();
                                                          // });
                                                          // scope.$on('$viewContentLoaded', function() {
                                                          //  scope.onResizeFunction();
                                                          //  //scope.$apply();
                                                          // });
            }
        };
    }
]);
'use strict';
angular.module('RedhatAccess.logViewer').directive('rhaLogtabs', function () {
    return {
        templateUrl: 'log_viewer/views/logTabs.html',
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
angular.module('RedhatAccess.logViewer').directive('rhaLogsinstructionpane', function () {
    return {
        templateUrl: 'log_viewer/views/logsInstructionPane.html',
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
angular.module('RedhatAccess.logViewer').directive('rhaNavsidebar', function () {
    return {
        templateUrl: 'log_viewer/views/navSideBar.html',
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
angular.module('RedhatAccess.logViewer').directive('rhaRecommendations', function () {
    return {
        templateUrl: 'log_viewer/views/recommendations.html',
        restrict: 'A',
        link: function postLink(scope, element, attrs) {
        }
    };
});
'use strict';
angular.module('RedhatAccess.logViewer').service('accordian', function () {
    var groups = [];
    return {
        getGroups: function () {
            return groups;
        },
        addGroup: function (group) {
            groups.push(group);
        },
        clearGroups: function () {
            groups = '';
        }
    };
});
'use strict';
angular.module('RedhatAccess.logViewer').factory('files', function () {
    var fileList = '';
    var selectedFile = '';
    var file = '';
    var retrieveFileButtonIsDisabled = { check: true };
    var fileClicked = { check: false };
    var activeTab = null;
    return {
        getFileList: function () {
            return fileList;
        },
        setFileList: function (fileList) {
            this.fileList = fileList;
        },
        getSelectedFile: function () {
            return selectedFile;
        },
        setSelectedFile: function (selectedFile) {
            this.selectedFile = selectedFile;
        },
        getFile: function () {
            return file;
        },
        setFile: function (file) {
            this.file = file;
        },
        setRetrieveFileButtonIsDisabled: function (isDisabled) {
            retrieveFileButtonIsDisabled.check = isDisabled;
        },
        getRetrieveFileButtonIsDisabled: function () {
            return retrieveFileButtonIsDisabled.check;
        },
        setFileClicked: function (isClicked) {
            fileClicked.check = isClicked;
        },
        getFileClicked: function () {
            return fileClicked;
        },
        setActiveTab: function (activeTab) {
            this.activeTab = activeTab;
        },
        getActiveTab: function () {
            return activeTab;
        }
    };
});
angular.module('RedhatAccess.template', ['common/views/alert.html', 'common/views/header.html', 'common/views/title.html', 'common/views/treenode.html', 'common/views/treeview-selector.html', 'security/views/login_form.html', 'security/views/login_status.html', 'search/views/accordion_search.html', 'search/views/accordion_search_results.html', 'search/views/list_search_results.html', 'search/views/resultDetail.html', 'search/views/search.html', 'search/views/search_form.html', 'search/views/standard_search.html', 'cases/views/accountSelect.html', 'cases/views/addCommentSection.html', 'cases/views/attachLocalFile.html', 'cases/views/attachProductLogs.html', 'cases/views/attachmentsSection.html', 'cases/views/chatButton.html', 'cases/views/commentsSection.html', 'cases/views/compact.html', 'cases/views/compactCaseList.html', 'cases/views/compactEdit.html', 'cases/views/createGroupButton.html', 'cases/views/createGroupModal.html', 'cases/views/defaultGroup.html', 'cases/views/deleteGroupButton.html', 'cases/views/descriptionSection.html', 'cases/views/detailsSection.html', 'cases/views/edit.html', 'cases/views/editGroup.html', 'cases/views/emailNotifySelect.html', 'cases/views/entitlementSelect.html', 'cases/views/exportCSVButton.html', 'cases/views/group.html', 'cases/views/groupList.html', 'cases/views/groupSelect.html', 'cases/views/list.html', 'cases/views/listAttachments.html', 'cases/views/listBugzillas.html', 'cases/views/listFilter.html', 'cases/views/listNewAttachments.html', 'cases/views/new.html', 'cases/views/newRecommendationsSection.html', 'cases/views/ownerSelect.html', 'cases/views/productSelect.html', 'cases/views/recommendationsSection.html', 'cases/views/requestManagementEscalationModal.html', 'cases/views/search.html', 'cases/views/searchBox.html', 'cases/views/searchResult.html', 'cases/views/selectLoadingIndicator.html', 'cases/views/severitySelect.html', 'cases/views/statusSelect.html', 'cases/views/typeSelect.html', 'log_viewer/views/logTabs.html', 'log_viewer/views/log_viewer.html', 'log_viewer/views/logsInstructionPane.html', 'log_viewer/views/navSideBar.html', 'log_viewer/views/recommendations.html']);

angular.module("common/views/alert.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("common/views/alert.html",
    "<div class=\"container-fluid\">\n" +
    "    <div class=\"row\" style=\"padding-bottom: 5px;\">\n" +
    "        <div class=\"col-xs-12\">\n" +
    "            <a style=\"float: right\" ng-show=\"AlertService.alerts.length > 1\" ng-href=\"\" ng-click=\"dismissAlerts()\">{{'Close messages'|translate}}</a>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-12\">\n" +
    "            <div alert ng-repeat='alert in AlertService.alerts' type='alert.type' close='closeAlert($index)'>{{alert.message}}</div>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("common/views/header.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("common/views/header.html",
    "<div class=\"rha-page-header\">\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-12\">\n" +
    "            <div rha-titletemplate page=\"{{page}}\"/>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-12\">\n" +
    "            <div rha-loginstatus />\n" +
    "        </div>\n" +
    "    </div>\n" +
    "    <div class=\"rha-bottom-border\" />\n" +
    "    <div class=\"row\">\n" +
    "        <div class=\"col-xs-12\">\n" +
    "            <div rha-alert />\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("common/views/title.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("common/views/title.html",
    "<h1 ng-show='showTitle'>{{titlePrefix}}{{getPageTitle()}}</h1>\n" +
    "");
}]);

angular.module("common/views/treenode.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("common/views/treenode.html",
    "<li class=\"rha-treeselector-node\">\n" +
    "    <div>\n" +
    "        <span class=\"icon\" ng-class=\"{collapsed: choice.collapsed, expanded: !choice.collapsed}\" ng-show=\"choice.children.length > 0\" ng-click=\"choice.collapsed = !choice.collapsed\">\n" +
    "        </span>\n" +
    "        <span class=\"label\" ng-if=\"choice.children.length > 0\" ng-class=\"folder\">{{choice.name}}\n" +
    "        </span>\n" +
    "        <span class=\"label\" ng-if=\"choice.children.length === 0\"  ng-click=\"choiceClicked(choice)\">\n" +
    "            <input type=\"checkbox\" ng-checked=\"choice.checked\">{{choice.name}}\n" +
    "        </span>\n" +
    "    </div>\n" +
    "</li>");
}]);

angular.module("common/views/treeview-selector.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("common/views/treeview-selector.html",
    "<div ng-controller=\"TreeViewSelectorCtrl\">\n" +
    "	<div> {{'Choose File(s) To Attach:'|translate}} </div>\n" +
    "  <rha-choice-tree ng-model=\"attachmentTree\"></rha-choice-tree>\n" +
    "  <pre>{{attachmentTree| json}}</pre>\n" +
    "</div>");
}]);

angular.module("security/views/login_form.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("security/views/login_form.html",
    "<div class=\"modal-header\" id=\"rha-login-modal-header\">\n" +
    "    <h3 translate>\n" +
    "        Sign into the Red Hat Customer Portal\n" +
    "    </h3>\n" +
    "</div>\n" +
    "<div class=\"container-fluid\">\n" +
    "    <div class=\"modal-body form-horizontal\" id=\"rha-login-modal-body\">\n" +
    "        <!--form ng-submit=\"modalOptions.ok()\"  method=\"post\"-->\n" +
    "        <div class=\"form-group\" ng-show='useVerboseLoginView'>\n" +
    "        {{'Red Hat Access makes it easy for you to self-solve issues, diagnose problems, and engage with us via the Red Hat Customer Portal. To access Red Hat Customer Portal resources, you must enter valid portal credentials.'|translate}}\n" +
    "        </div>\n" +
    "\n" +
    "        <div class=\"alert alert-danger\" ng-show=\"authError\">\n" +
    "            {{authError}}\n" +
    "        </div>\n" +
    "        <div class=\"form-group\" id=\"rha-login-modal-user-id\">\n" +
    "            <label for=\"rha-login-user-id\" class=\" control-label\" translate>Red Hat Login</label>\n" +
    "            <div>\n" +
    "                <input type=\"text\" class=\"form-control\" id=\"rha-login-user-id\" placeholder=\"{{'Red Hat Login'|translate}}\"  ng-model=\"user.user\" required autofocus>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <div class=\"form-group\" id=\"rha-login-modal-user-pass\">\n" +
    "            <label for=\"rha-login-password\" class=\"control-label\" translate>Password</label>\n" +
    "            <div>\n" +
    "                <input type=\"password\" class=\"form-control\" id=\"rha-login-password\" placeholder=\"{{'Password'|translate}}\" ng-model=\"user.password\" required>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <div class=\"form-group\" style=\"font-size:smaller\" ng-show='useVerboseLoginView'>\n" +
    "            <strong>{{'Note:'|translate}}\n" +
    "                &nbsp;</strong>{{'Red Hat Customer Portal credentials differ from the credentials used to log into this product.'|translate}}\n" +
    "        </div>\n" +
    "\n" +
    "        <!--/form-->\n" +
    "    </div>\n" +
    "    <div class=\"modal-footer\">\n" +
    "        <div class=\"form-group\" id=\"rha-login-modal-buttons\">\n" +
    "            <span class=\"pull-right\">\n" +
    "                <button class=\"btn  btn-md cancel\" ng-click=\"modalOptions.close()\" type=\"submit\" translate>Cancel</button>\n" +
    "                <button class=\"btn btn-primary btn-md login\" ng-click=\"modalOptions.ok()\" type=\"submit\" translate ng-disabled=\"status.authenticating\">Sign in</button>\n" +
    "            </span>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>");
}]);

angular.module("security/views/login_status.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("security/views/login_status.html",
    "<div ng-controller = 'SecurityController' ng-show=\"displayLoginStatus()\">\n" +
    "<span ng-show=\"securityService.loginStatus.isLoggedIn\" class=\"pull-right rha-logged-in\"> {{'Logged into the Red Hat Customer Portal as'|translate}} {{securityService.loginStatus.authedUser.loggedInUser}} &nbsp;|&nbsp;\n" +
    "    <span ng-if=\"securityService.logoutURL.length === 0\" ng-show=\"!securityService.loginStatus.verifying\">\n" +
    "        <a href=\"\" ng-click=\"securityService.logout()\"> {{'Log Out'|translate}}</a>\n" +
    "    </span>\n" +
    "    <span ng-if=\"securityService.logoutURL.length > 0\" ng-show=\"!securityService.loginStatus.verifying\">\n" +
    "        <a href=\"{{securityService.logoutURL}}\"> {{'Log Out'|translate}}</a>\n" +
    "    </span>\n" +
    "    <span ng-show=\"securityService.loginStatus.verifying\" >\n" +
    "         {{'Log Out'|translate}}\n" +
    "    </span>\n" +
    "</span>\n" +
    "<span ng-show=\"!securityService.loginStatus.isLoggedIn\" class=\"pull-right rha-logged-out\"> {{'Not Logged into the Red Hat Customer Portal'|translate}}&nbsp;|&nbsp;\n" +
    "    <span ng-if=\"securityService.loginURL.length === 0\" ng-show=\"!securityService.loginStatus.verifying\">\n" +
    "        <a href=\"\" ng-click=\"securityService.login()\"> {{'Log In'|translate}}</a>\n" +
    "    </span>\n" +
    "    <span ng-if=\"securityService.loginURL.length > 0\" ng-show=\"!securityService.loginStatus.verifying\">\n" +
    "        <a href=\"{{securityService.loginURL}}\"> {{'Log In'|translate}}</a>\n" +
    "    </span>\n" +
    "    <span ng-show=\"securityService.loginStatus.verifying\">\n" +
    "        {{'Log In'|translate}}\n" +
    "    </span>\n" +
    "</span>\n" +
    "\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/accordion_search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/accordion_search.html",
    "<div class=\"container-fluid rha-side-padding\">\n" +
    "    <div rha-header title=\"Search\"></div>\n" +
    "    <div class=\"row\" rha-searchform ng-controller='SearchController'></div>\n" +
    "    <div style=\"padding-top: 10px;\"></div>\n" +
    "    <div class='row'>\n" +
    "    	<div class=\"container\" rha-accordionsearchresults='' ng-controller='SearchController' />\n" +
    "    </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/accordion_search_results.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/accordion_search_results.html",
    "<div class=\"row rha-bottom-border\">\n" +
    "    <div class=\"col-xs-6\">\n" +
    "        <div style=\"padding-bottom: 0\">\n" +
    "            <span>\n" +
    "                <h4 style=\"padding-left: 10px; display: inline-block;\" translate=''>Recommendations</h4>\n" +
    "            </span>\n" +
    "            <span ng-show=\"searchInProgress.value\" class=\"rha-search-spinner\">\n" +
    "                &nbsp;\n" +
    "            </span>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "    <div style=\"padding-bottom: 14px;\" class=\"col-xs-6\" ng-show=\"showOpenCaseBtn()\">\n" +
    "        <a href={{getOpenCaseRef()}} class=\"btn btn-primary pull-right \">{{'Open a New Support Case'|translate}}</a>\n" +
    "    </div>\n" +
    "</div>\n" +
    "<div class=\"\">\n" +
    "    <!--div class=\"col-xs-12\" style=\"overflow: auto;\" rha-resizable rha-dom-ready=\"domReady\"-->\n" +
    "        <div accordion=''>\n" +
    "            <div accordion-group='' is-open=\"isopen\" ng-click=\"triggerAnalytics($event)\" ng-repeat=\"result in results track by $index\">\n" +
    "                <div accordion-heading=''>\n" +
    "                    <span class=\"pull-left glyphicon\" ng-class=\"{'glyphicon-chevron-down': isopen, 'glyphicon-chevron-right': !isopen}\"></span>\n" +
    "                    <span class=\"result-title\">&nbsp{{result.title}}</span>\n" +
    "                </div>\n" +
    "                <div rha-resultdetaildisplay result='result' />\n" +
    "            </div>\n" +
    "        </div>\n" +
    "    <!--/div-->\n" +
    "</div>\n" +
    "");
}]);

angular.module("search/views/list_search_results.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/list_search_results.html",
    "<div class=\"col-sm-4\">\n" +
    "    <div class=\"panel panel-default\" ng-show='results.length > 0'>\n" +
    "        <!--pagination on-select-page=\"pageChanged(page)\" total-items=\"totalItems\" page=\"currentPage\" max-size=\"maxSize\"></pagination-->\n" +
    "\n" +
    "        <div class=\"panel-heading\">\n" +
    "            <h4 class=\"panel-title\" translate=''>\n" +
    "                Recommendations\n" +
    "            </h4>\n" +
    "        </div>\n" +
    "        <div id='solutions' class=\"list-group\">\n" +
    "            <a href=\"\" ng-click=\"solutionSelected($index)\" class='list-group-item' ng-class=\"{'active': selectedSolution.index===$index}\" ng-repeat=\"result in results track by $index\" style=\"word-wrap: break-word;\"> <i class=\"glyphicon glyphicon-chevron-right pull-right\"></i>{{ result.title }}</a>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>\n" +
    "<div class=\"col-sm-8\" style=\"overflow: auto;\" rha-resizable rha-domready=\"domReady\">\n" +
    "    <div class=\"alert alert-info\" ng-show='selectedSolution.index === -1 && results.length > 0'>\n" +
    "        {{'To view a recommendation, click on it.'|translate}}\n" +
    "    </div>\n" +
    "    <div style \"overflow: vertical;\">\n" +
    "        <div rha-resultdetaildisplay result='selectedSolution.data' />\n" +
    "    </div>\n" +
    "</div>");
}]);

angular.module("search/views/resultDetail.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/resultDetail.html",
    "<div class='panel' style='border:0' ng-model=\"result\" >\n" +
    "	<div ng-if=\"isSolution()\">\n" +
    "		<h3 translate=''>Environment</h3>\n" +
    "		<div ng-bind-html='result.environment.html'></div>\n" +
    "		<h3>Issue</h3>\n" +
    "		<div ng-bind-html='result.issue.html'></div>\n" +
    "		<div ng-if=\"getSolutionResolution() !== ''\">\n" +
    "			<h3  translate=''>Resolution</h3>\n" +
    "		</div>\n" +
    "		<div ng-bind-html='getSolutionResolution()'></div>\n" +
    "	</div>\n" +
    "	<div ng-if=\"isArticle()\">\n" +
    "		<div ng-bind-html='getArticleHtml()'></div>\n" +
    "	</div>\n" +
    "</div>\n" +
    "\n" +
    "");
}]);

angular.module("search/views/search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search.html",
    "<div rha-standardsearch/>\n" +
    "");
}]);

angular.module("search/views/search_form.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/search_form.html",
    "<div class='col-sm-4 pull-left'>\n" +
    "    <form role=\"form\" id=\"rh-search\">\n" +
    "        <div ng-class=\"{'col-sm-8': searchInProgress.value}\">\n" +
    "            <div class=\"input-group\">\n" +
    "                <input type=\"text\" class=\"form-control\" id=\"rhSearchStr\" name=\"searchString\" ng-model=\"searchStr\" class=\"input-xxlarge\" placeholder=\"Search Articles and Solutions\">\n" +
    "                <span class=\"input-group-btn\">\n" +
    "                    <button ng-disabled=\"(searchStr === undefined || searchStr.trim()==='' || searchInProgress.value === true)\" class=\"btn btn-default btn-primary\" type='submit' ng-click=\"search(searchStr)\">\n" +
    "                        <i class=\"glyphicon glyphicon-search \"></i>\n" +
    "                        {{'Search'|translate}}</button>\n" +
    "                </span>\n" +
    "\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <div class=\"col-sm-4 \" ng-show=\"searchInProgress.value\">\n" +
    "            <span class=\"rha-search-spinner\">\n" +
    "                &nbsp;\n" +
    "            </span>\n" +
    "        </div>\n" +
    "\n" +
    "    </form>\n" +
    "</div>");
}]);

angular.module("search/views/standard_search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("search/views/standard_search.html",
    "<div class=\"container-fluid rha-side-padding\" ng-controller='SearchController'>\n" +
    "    <div rha-header page=\"search\"></div>\n" +
    "    <div class=\"row\" rha-searchform></div>\n" +
    "    <div style=\"padding-top: 10px;\"></div>\n" +
    "    <div class='row' rha-listsearchresults='' ng-controller='SearchController' />\n" +
    "</div>\n" +
    "");
}]);

angular.module("cases/views/accountSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/accountSelect.html",
    "<div style=\"display: inline-block; padding-right: 10px;\"><input id=\"rha-account-number\" style=\"width: 100%\" ng-model=\"CaseService.account.number\" ng-blur=\"populateAccountSpecificFields()\" class=\"form-control\"/></div><div style=\"display: inline-block;\"><button ng-click=\"selectUserAccount()\" ng-hide=\"loadingAccountNumber\" translate=\"\" class=\"btn btn-secondary\">My Account</button><span ng-show=\"loadingAccountNumber\" class=\"rha-search-spinner\">&nbsp;</span></div>");
}]);

angular.module("cases/views/addCommentSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/addCommentSection.html",
    "<div style=\"margin-left: 60px; margin-right: 60px;\"><div style=\"margin-bottom: 10px;\" class=\"well\"><textarea id=\"case-comment-box\" ng-disabled=\"addingComment\" rows=\"5\" ng-model=\"CaseService.commentText\" style=\"max-width: 100%\" ng-change=\"onNewCommentKeypress()\" class=\"form-control\"></textarea><span id=\"commentNotice\" class=\"uploadNotice\"> <span>{{'You have used'|translate}}</span><span class=\"progressBarWrap\"><span class=\"progressCount\">{{progressCount}} %</span></span><span>{{'of the 32KB maximum description size.'|translate}}</span></span><div style=\"padding-top: 10px;\"><div ng-if=\"securityService.loginStatus.authedUser.is_internal\"><span style=\"float: left;\">{{'Is Public:'|translate}}</span><input id=\"rha-case-comment-isPublic\" type=\"checkbox\" ng-model=\"CaseService.isCommentPublic\" style=\"display: inline-block;\"/></div><span ng-show=\"savingDraft\" class=\"pull-right rha-bold\">{{'Saving draft...'|translate}}</span><span ng-show=\"draftSaved &amp;&amp; !savingDraft\" class=\"pull-right rha-bold\">{{'Draft saved'|translate}}</span></div></div><div style=\"float: right;\"><span ng-show=\"addingComment\" class=\"rha-search-spinner\"></span><button id=\"rha-case-addcommentbutton\" ng-hide=\"addingComment\" ng-disabled=\"CaseService.disableAddComment\" ng-click=\"addComment()\" style=\"float: right;\" translate=\"\" class=\"btn btn-primary\">Add Comment</button></div></div>");
}]);

angular.module("cases/views/attachLocalFile.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/attachLocalFile.html",
    "<div class=\"container-fluid\"><div class=\"row rha-create-field\"><div class=\"col-xs-6\"><button style=\"float: left;\" ng-click=\"getFile()\" ng-disabled=\"disabled\" translate=\"\" class=\"btn btn-attach\">Attach local file</button><div style=\"height: 0px; width:0px; overflow:hidden;\"><input id=\"fileUploader\" type=\"file\" value=\"upload\" ng-model=\"file\" ng-disabled=\"disabled\"/></div></div><div class=\"col-xs-6\"><div style=\"float: left; word-wrap: break-word; width: 100%;\">{{fileName}}</div></div></div><div class=\"row rha-create-field\"><div style=\"font-size: 80%;\" class=\"col-xs-12\">     <div ng-bind-html=\"parseArtifactHtml()\"></div></div><div style=\"font-size: 80%;\" class=\"col-xs-12\"><span>{{'File names must be less than 80 characters. Maximum file size for web-uploaded attachments is 250 MB. Please FTP larger files to dropbox.redhat.com.'|translate}}&nbsp;</span><span><a href=\"https://access.redhat.com/knowledge/solutions/2112\" target=\"_blank\">(More info)</a></span></div></div><div class=\"row rha-create-field\"><div class=\"col-xs-12\"><input id=\"rha-case-attachement-fileDescription\" style=\"float: left;\" placeholder=\"File description\" ng-model=\"fileDescription\" ng-disabled=\"disabled\" class=\"form-control\"/></div></div><div class=\"row rha-create-field\"><div class=\"col-xs-12\"><button ng-disabled=\"fileName == NO_FILE_CHOSEN || disabled\" style=\"float: right;\" ng-click=\"addFile(fileUploaderForm)\" translate=\"\" class=\"btn btn-add\">Add</button></div></div></div>");
}]);

angular.module("cases/views/attachProductLogs.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/attachProductLogs.html",
    "<div class=\"container-fluid\"><div class=\"row rha-create-field\"><div class=\"col-xs-12\"><div style=\"padding-bottom: 4px;\">{{'Attach Foreman logs:'|translate}}</div><select multiple=\"multiple\" class=\"form-control\"><option>Log1</option><option>Log2</option><option>Log3</option><option>Log4</option><option>Log5</option><option>Log6</option></select></div></div><div class=\"row rha-create-field\"><div class=\"col-xs-12\"><button ng-disabled=\"true\" style=\"float: right;\" translate=\"\" class=\"btn\">Add</button></div></div></div>");
}]);

angular.module("cases/views/attachmentsSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/attachmentsSection.html",
    "<h4 translate=\"\" class=\"rha-section-header\">Attachments</h4><span ng-show=\"loading\" class=\"rha-search-spinner\"></span><div ng-hide=\"loading\" class=\"container-fluid rha-side-padding\"><div class=\"row rha-side-padding\"><div class=\"col-xs-12 rha-col-no-padding\"><div rha-listattachments=\"\"></div></div></div><div style=\"border-top: 1px solid #cccccc; padding-top: 10px; margin: 0;\" class=\"row\"></div><div ng-hide=\"AttachmentsService.updatedAttachments.length &lt;= 0 &amp;&amp; TreeViewSelectorUtils.getSelectedLeaves(AttachmentsService.backendAttachments).length &lt;= 0\"><div class=\"row rha-side-padding\"><div class=\"col-xs-12 rha-col-no-padding\"><div rha-listnewattachments=\"rha-listnewattachments\"></div></div></div><div class=\"row rha-side-padding\"><div style=\"padding-bottom: 14px;\" class=\"col-xs-12 rha-col-no-padding\"><div style=\"float: right\"><span ng-show=\"updatingAttachments\" class=\"rha-search-spinner\"></span><button ng-hide=\"updatingAttachments\" ng-click=\"doUpdate()\" translate=\"\" class=\"btn btn-primary\">Upload Attachments</button></div></div></div><div style=\"border-top: 1px solid #cccccc; padding-top: 10px; margin: 0;\" class=\"row\"></div></div><div class=\"row\"><div class=\"col-xs-12\"><div rha-attachlocalfile=\"\"></div></div></div><div ng-show=\"showServerSideAttachments\"><div class=\"row\"><div class=\"col-xs-12\"><div class=\"server-attach-header\">{{'Server File(s) To Attach:'|translate}}</div><div rha-choicetree=\"\" ng-model=\"attachmentTree\" ng-controller=\"BackEndAttachmentsCtrl\" rhaDisabled=\"rhaDisabled\">     </div></div></div></div></div>");
}]);

angular.module("cases/views/chatButton.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/chatButton.html",
    "<span ng-show=\"showChat\"><iframe style=\"display: none;\" ng-src=\"{{chatHackUrl}}\"></iframe><button ng-show=\"chatAvailable\" ng-click=\"openChatWindow()\" translate=\"\" class=\"btn btn-primary btn-slim btn-sm\">Chat with support</button><button ng-show=\"!chatAvailable\" disabled=\"disabled\" translate=\"\" class=\"btn btn-secondary btn-slim btn-sm\">Chat offline</button></span>");
}]);

angular.module("cases/views/commentsSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/commentsSection.html",
    "<h4 translate=\"\" class=\"rha-section-header\">Case Discussion</h4><span ng-show=\"loading\" class=\"rha-search-spinner\"></span><div ng-hide=\"loading\" class=\"container-fluid rha-side-padding\"><div class=\"row rha-create-field\"><div class=\"col-xs-12\"><div rha-addcommentsection=\"\"></div></div></div><div style=\"border-top: 1px solid #cccccc; padding-top: 10px; padding-bottom: 10px;\" class=\"row\"><div class=\"col-xs-12\"><span style=\"display: inline-block; padding-right: 10px;\">{{'Would you like a Red Hat support manager to contact you regarding this case?'|translate}}</span><button style=\"display: inline-block\" ng-click=\"requestManagementEscalation()\" translate=\"\" class=\"btn btn-secondary\">Request Management Escalation</button></div></div><div ng-hide=\"CaseService.comments.length &lt;= 0 || CaseService.comments === undefined\" style=\"border-top: 1px solid #dddddd;\" class=\"rha-comments-section\"><div ng-repeat=\"comment in CaseService.comments\" ng-if=\"!comment.draft\"><div id=\"{{comment.id}}\"><div style=\"padding-bottom: 10px;\" class=\"row\"><div class=\"col-md-2\"><div class=\"rha-bold personNameBlock\">{{comment.created_by}}</div><div>{{comment.created_date | date:'mediumDate'}}</div><div>{{comment.created_date | date:'h:mm:ss a Z'}}</div><div ng-if=\"comment.public !== undefined &amp;&amp; comment.public === false\" class=\"private\">Private</div></div><div class=\"col-md-9 rha-comment-text\"><pre style=\"word-break: normal;\" ng-bind-html=\"comment.text | linky:'_blank'\" class=\"pcmTextBlock\"></pre><a ng-click=\"commentReply(comment.id)\" class=\"commentReply\">{{'Reply'|translate}}</a></div><div class=\"col-md-1 rha-comment-link\"><a ng-click=\"CaseService.scrollToComment(comment.id)\" ng-href=\"#/case/{{CaseService.kase.case_number}}?commentId={{comment.id}}\" class=\"glyphicon glyphicon-link\"></a></div></div></div></div></div></div>");
}]);

angular.module("cases/views/compact.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/compact.html",
    "<div class=\"container-offset\">\n" +
    "    <div class=\"container-fluid\">\n" +
    "        <div class=\"row\">\n" +
    "            <div class=\"col-xs-12\">\n" +
    "                <div rha-header page=\"caseList\"/>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "        <div class=\"row\">\n" +
    "            <div class=\"col-xs-4\" style=\"height: 100%;\">\n" +
    "                <div rha-compactcaselist></div>\n" +
    "            </div>\n" +
    "            <div class=\"col-xs-8\" style=\"padding: 0px; \">\n" +
    "                <!-- Jade can't create the ui-view attribute in the form\n" +
    "                     angular ui router requires (see next line).-->\n" +
    "                <div ui-view autoscroll=\"false\"></div>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("cases/views/compactCaseList.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/compactCaseList.html",
    "<div id=\"redhat-access-case\"><div id=\"redhat-access-compact-list\" class=\"container-fluid\"><div class=\"row\"><div class=\"col-xs-12 rha-col-no-padding\"><div rha-listfilter=\"\"></div></div></div><div class=\"row\"><div class=\"col-xs-12\"><div ng-show=\"SearchCaseService.cases.length == 0 &amp;&amp; !SearchCaseService.searching &amp;&amp; securityService.loginStatus.isLoggedIn\">{{'No cases found with given filters.'|translate}}</div><span ng-show=\"SearchCaseService.searching &amp;&amp; securityService.loginStatus.isLoggedIn\" class=\"rha-search-spinner\"></span></div></div><div ng-hide=\"SearchCaseService.cases.length == 0 || SearchCaseService.searching\" style=\"border-top: 1px solid #dddddd;\" class=\"row\"><div style=\"overflow: auto;\" rha-resizable=\"rha-resizable\" rha-domready=\"domReady\" class=\"col-xs-12 rha-col-no-padding\"><div style=\"margin-bottom: 0px; overflow: auto;\"><ul style=\"margin-bottom: 0px;\" class=\"list-group\"><a ng-repeat=\"case in SearchCaseService.cases\" ui-sref=\".edit({id: &quot;{{case.case_number}}&quot;})\" ng-class=\"{&quot;active&quot;: $index == selectedCaseIndex}\" ng-click=\"selectCase($index)\" class=\"list-group-item\">{{case.case_number}} {{case.summary}}</a></ul></div></div></div></div></div>");
}]);

angular.module("cases/views/compactEdit.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/compactEdit.html",
    "<!DOCTYPE html><div id=\"redhat-access-case\"><div ng-show=\"caseLoading &amp;&amp; securityService.loginStatus.isLoggedIn\" class=\"container-fluid\"><div style=\"margin-right: 0px;\" class=\"row\"><div class=\"col-xs-12\"><span class=\"rha-search-spinner\"></span></div></div></div><div ng-hide=\"caseLoading\" rha-resizable rha-dom-ready=\"domReady\" style=\"overflow: auto; padding-left: 15px;border-top: 1px solid #dddddd; border-left: 1px solid #dddddd;\" class=\"container-fluid\"><div style=\"margin-right: 0px; padding-top: 10px;\" class=\"row\"><div class=\"col-xs-12\"><div rha-casedetails=\"\" compact=\"true\"></div></div></div><div style=\"margin-right: 0px;\" class=\"row\"><div class=\"col-xs-12\"><div rha-casedescription=\"\"></div></div></div><div style=\"margin-right: 0px;\" class=\"row\"><div class=\"col-xs-12\"><div rha-case-attachments=\"\"></div></div></div><div style=\"margin-right: 0px;\" class=\"row\"><div class=\"col-xs-12\"><div rha-casecomments=\"\"></div></div></div></div></div>");
}]);

angular.module("cases/views/createGroupButton.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/createGroupButton.html",
    "<button ng-click=\"openCreateGroupDialog()\" translate=\"\" class=\"btn btn-primary\">Create New Case Group</button>");
}]);

angular.module("cases/views/createGroupModal.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/createGroupModal.html",
    "<div id=\"rha-create-group-modal\"><div class=\"modal-header\"><h3 translate=\"\">Create Case Group</h3></div><div style=\"padding: 20px;\" class=\"container-fluid\"><div style=\"padding-bottom: 20px;\" class=\"row\"><div class=\"col-sm-12\"><div style=\"display: table; width: 100%;\"><label style=\"display: table-cell\" translate=\"\">Case Group:</label><input id=\"rha-case-groupName\" ng-model=\"groupName\" style=\"display: table-cell; width: 100%;\" ng-keypress=\"onGroupNameKeyPress($event)\" class=\"form-control\"/></div></div></div><div class=\"row\"><div class=\"col-sm-12\"><button ng-click=\"createGroup()\" style=\"margin-left: 10px;\" translate=\"\" class=\"btn-primary btn pull-right\">Save</button><button ng-click=\"closeModal()\" translate=\"\" class=\"btn-secondary btn pull-right\">Cancel</button></div></div></div></div>");
}]);

angular.module("cases/views/defaultGroup.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/defaultGroup.html",
    "<div id=\"redhat-access-case\" class=\"container-offset\"><div rha-header=\"\" page=\"defaultGroup\" ng-controller=\"DefaultGroup\"></div><div class=\"rha-side-padding\"><div style=\"padding-bottom: 20px;\" class=\"row\"><div style=\"padding-bottom: 20px;\" class=\"col-xs-12\"><div class=\"col-xs-2\"><label>Case Group Name: </label></div><div class=\"col-xs-9\"><select id=\"rha-defaultgroup-groupselect\" chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn || groupsLoading\" ng-model=\"selectedGroup\" ng-options=\"group as group.name for group in groups\" class=\"form-control\"></select></div><div class=\"col-xs-1\"><div style=\"width: 100%\"><span ng-show=\"groupsLoading\" class=\"rha-search-spinner\"></span></div></div></div><div style=\"padding-bottom: 20px;\" class=\"col-xs-12\"><div class=\"col-xs-2\"><label>Group Users: </label></div><div class=\"col-xs-9\"><select id=\"rha-defaultgroup-userselect\" chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn || !usersLoaded\" ng-model=\"selectedUser\" ng-change=\"userChange()\" ng-options=\"user.sso_username for user in usersOnAccount\" class=\"form-control\"></select></div><div class=\"col-xs-1\"><div style=\"width: 100%\"><span ng-show=\"usersLoading\" class=\"rha-search-spinner\"></span></div></div></div><div style=\"padding-bottom: 20px;\" class=\"col-xs-12\"><div style=\"padding-bottom: 20px;\" class=\"row\"></div><button ng-click=\"setDefaultGroup()\" ng-disabled=\"!usersAndGroupsFinishedLoading\" translate=\"\" class=\"btn btn-primary\">Save Group</button><button ng-click=\"back()\" translate=\"\" class=\"btn btn-primary\">Back</button></div></div></div></div>");
}]);

angular.module("cases/views/deleteGroupButton.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/deleteGroupButton.html",
    "<button ng-click=\"deleteGroups()\" ng-disabled=\"GroupService.disableDeleteGroup\" translate=\"\" class=\"btn btn-secondary\">Delete Group</button>");
}]);

angular.module("cases/views/descriptionSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/descriptionSection.html",
    "<h4 translate=\"\" class=\"rha-section-header\">Description</h4><span ng-show=\"loading\" class=\"rha-search-spinner\"></span><div ng-hide=\"loading\" class=\"container-fluid rha-side-padding\"><div class=\"row\"><div class=\"col-md-2\"><strong>{{CaseService.kase.created_by}}</strong></div><div class=\"col-md-10 pcmTextBlock\">{{CaseService.kase.description}}</div></div></div>");
}]);

angular.module("cases/views/detailsSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/detailsSection.html",
    "<form name=\"caseDetails\"><div style=\"display: table; width: 100%; padding-bottom: 20px;\"><div style=\"display: table-cell; width: 50%;\"><div><h3 style=\"margin-top: 0px;\" class=\"case-id\">Case {{CaseService.kase.case_number}} <span ng-show=\"CaseService.kase.entitlement.sla=='AMC'\" class=\"amc\">{{'Advanced Mission Critical'|translate}}</span></h3></div><input style=\"width: 100%; display: inline-block;\" ng-model=\"CaseService.kase.summary\" name=\"summary\" class=\"form-control\"/><span ng-show=\"caseDetails.summary.$dirty\" style=\"display: inline-block;\" class=\"glyphicon glyphicon-asterisk\"></span></div><div ng-hide=\"compact\" style=\"display: table-cell; vertical-align: bottom; width: 50%;\"><div ng-show=\"showEmailNotifications\"><div style=\"width: 75%\" class=\"pull-right\"><div rha-emailnotifyselect=\"\"></div></div></div></div></div><span ng-show=\"loading\" class=\"rha-search-spinner\"></span><div ng-hide=\"loading\" class=\"container-fluid rha-side-padding\"><div id=\"rha-case-details\" class=\"row\"><div class=\"col-sm-12 rha-section-header\"><h4 translate=\"\">Details</h4></div><div class=\"container-fluid rha-side-padding\"><div class=\"row\"><div class=\"col-md-4\"><table class=\"table details-table\"><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-case-type\">{{'Case Type:'|translate}}</label></div><span ng-show=\"caseDetails.type.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><div rha-selectloadingindicator=\"\" loading=\"caseTypes === undefined\" type=\"bootstrap\"><select id=\"rha-case-type\" name=\"type\" style=\"width: 100%;\" ng-model=\"CaseService.kase.type\" ng-options=\"c.name for c in caseTypes track by c.name\" class=\"form-control\"></select></div></td></tr><tr><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-case-severity\">{{'Severity:'|translate}}</label></div><span ng-show=\"caseDetails.severity.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><div rha-selectloadingindicator=\"\" loading=\"CaseService.severities === undefined\" type=\"bootstrap\"><select id=\"rha-case-severity\" name=\"severity\" style=\"width: 100%;\" ng-model=\"CaseService.kase.severity\" ng-options=\"s.name for s in CaseService.severities track by s.name\" class=\"form-control\"></select></div></td></tr><tr ng-show=\"CaseService.showFts()\"><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\">{{'24x7 Support:'|translate}}</div></th><td><input id=\"rha-case-ftsCheckboxEdit\" ng-model=\"CaseService.kase.fts\" type=\"checkbox\"/></td></tr><tr ng-show=\"CaseService.showFts() &amp;&amp; CaseService.kase.fts\"><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\">{{'24x7 Contact:'|translate}}</div></th><td><input id=\"rha-case-contact-24x7-edit\" ng-model=\"CaseService.kase.contact_info24_x7\" class=\"form-control\"/></td></tr><tr><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-case-status\">{{'Status:'|translate}}</label></div><span ng-show=\"caseDetails.status.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><div rha-selectloadingindicator=\"\" loading=\"statuses === undefined\" type=\"bootstrap\"><select id=\"rha-case-status\" name=\"status\" style=\"width: 100%;\" ng-model=\"CaseService.kase.status\" ng-options=\"s.name for s in statuses track by s.name\" class=\"form-control\"></select></div></td></tr><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-case-alt-id\">{{'Alternate Case ID:'|translate}}</label></div><span ng-show=\"caseDetails.alternate_id.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><input id=\"rha-case-alt-id\" style=\"width: 100%\" ng-model=\"CaseService.kase.alternate_id\" name=\"alternate_id\" class=\"form-control\"/></td></tr></table></div><div class=\"col-md-4\"><table class=\"table details-table\"><tr><th><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-product\">{{'Product:'|translate}}</label></div><span ng-show=\"caseDetails.product.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><div rha-selectloadingindicator=\"\" loading=\"products === undefined\" type=\"bootstrap\"><select id=\"rha-product\" name=\"product\" style=\"width: 100%;\" ng-model=\"CaseService.kase.product\" ng-change=\"getProductVersions()\" ng-options=\"s.name for s in products track by s.name\" required=\"required\" class=\"form-control\"></select></div></td></tr><tr><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-product-version\">{{'Product Version:'|translate}}</label></div><span ng-show=\"caseDetails.version.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><div rha-selectloadingindicator=\"\" loading=\"CaseService.versions.length === 0\" type=\"bootstrap\"><select id=\"rha-product-version\" name=\"version\" style=\"width: 100%;\" ng-options=\"v for v in CaseService.versions track by v\" ng-model=\"CaseService.kase.version\" required=\"required\" class=\"form-control\"></select></div></td></tr><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><label for=\"rha-support-level\">{{'Support Level:'|translate}}</label></th><td id=\"rha-support-level\">{{CaseService.kase.entitlement.sla}}</td></tr><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><label for=\"rha-owner\">{{'Owner:'|translate}}</label></th><td id=\"rha-owner\">{{CaseService.kase.contact_name}} <{{CaseService.kase.contact_sso_username }}></td></tr><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><label for=\"rha-rh-owner\">{{'Red Hat Owner:'|translate}}</label></th><td id=\"rha-rh-owner\">{{CaseService.kase.owner}}</td></tr></table></div><div class=\"col-md-4\"><table class=\"table details-table\"><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><div style=\"vertical-align: 50%; display: inline-block;\"><label for=\"rha-group-select\">{{'Group:'|translate}}</label></div><span ng-show=\"caseDetails.group.$dirty\" style=\"display: inline-block;float: right; vertical-align: 50%;\" class=\"glyphicon glyphicon-asterisk\"></span></th><td><div rha-selectloadingindicator=\"\" loading=\"groups === undefined\" type=\"bootstrap\"><select id=\"rha-group-select\" name=\"group\" style=\"width: 100%;\" ng-options=\"g.name for g in groups track by g.number\" ng-model=\"CaseService.kase.group\" class=\"form-control\"><option value=\"\">Ungrouped Case</option></select></div></td></tr><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><label for=\"rha-opened\">{{'Opened:'|translate}}</label></th><td id=\"rha-opened\"><div>{{CaseService.kase.created_date | date:'MMM d, y h:mm:ss a Z'}}</div><div>{{CaseService.kase.created_by}}</div></td></tr><tr ng-hide=\"compact\"><th class=\"rha-detail-table-header\"><label for=\"rha-last-updated\">{{'Last Updated:'|translate}}</label></th><td id=\"rha-last-updated\"><div>{{CaseService.kase.last_modified_date | date:'MMM d, y h:mm:ss a Z'}}</div><div>{{CaseService.kase.last_modified_by}}</div></td></tr><tr ng-hide=\"compact\" ng-if=\"securityService.loginStatus.authedUser.is_internal\" class=\"rha-detail-acc-number\"><th class=\"rha-detail-table-header\"><label for=\"rha-account-number\">{{'Account Number:'|translate}}</label></th><td id=\"rha-account-number\">{{CaseService.kase.account_number}}</td></tr><tr ng-hide=\"compact\" ng-if=\"securityService.loginStatus.authedUser.is_internal\" class=\"rha-detail-acc-name\"><th class=\"rha-detail-table-header\"><label for=\"rha-account-name\">{{'Account Name:'|translate}}</label></th><td id=\"rha-account-name\">{{CaseService.account.name}}        </td></tr></table></div></div><div ng-if=\"!securityService.loginStatus.authedUser.is_internal\"><label for=\"rha-case-notes\">{{'Notes:'|translate}} </label><span ng-show=\"caseDetails.notes.$dirty\" class=\"glyphicon glyphicon-asterisk\"></span><textarea id=\"rha-case-notes\" style=\"width: 100%; height: 100px; max-width: 100%;\" ng-model=\"CaseService.kase.notes\" name=\"notes\"></textarea></div><div style=\"padding-top: 10px;\" class=\"row\"><div class=\"col-xs-12\"><div style=\"float: right;\"><button id=\"rha-caseupdateform-updatebutton\" name=\"updateButton\" ng-disabled=\"!caseDetails.$dirty\" ng-hide=\"updatingDetails\" ng-click=\"updateCase()\" translate=\"\" class=\"btn btn-primary\">Update Details</button><span ng-show=\"updatingDetails\" class=\"rha-search-spinner\"></span></div></div></div></div></div></div></form>");
}]);

angular.module("cases/views/edit.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/edit.html",
    "<!DOCTYPE html><div id=\"redhat-access-case\" class=\"container-offset\"><div rha-header=\"\" page=\"caseView\"></div><div ng-show=\"securityService.loginStatus.isLoggedIn\" class=\"container-fluid rha-side-padding\"><div ng-show=\"securityService.loginStatus.isLoggedIn &amp;&amp; securityService.loginStatus.authedUser.has_chat\" class=\"row\"><div class=\"pull-right\"><div rha-chatbutton=\"\"></div></div></div><div ng-show=\"EDIT_CASE_CONFIG.showDetails\" class=\"row\"><div class=\"col-xs-12\"><div rha-casedetails=\"\" compact=\"false\" loading=\"loading.kase\"></div></div></div><div ng-show=\"EDIT_CASE_CONFIG.showDescription\" class=\"row\"><div class=\"col-xs-12\"><div rha-casedescription=\"\" loading=\"loading.kase\"></div></div></div><div ng-show=\"EDIT_CASE_CONFIG.showBugzillas\" class=\"row\"><div class=\"col-xs-12\"><div rha-listbugzillas=\"\" loading=\"loading.kase\"></div></div></div><div ng-show=\"EDIT_CASE_CONFIG.showAttachments &amp;&amp; securityService.loginStatus.authedUser.can_add_attachments\" class=\"row\"><div class=\"col-xs-12\"><div rha-caseattachments=\"\" loading=\"loading.attachments\"></div></div></div><div ng-show=\"EDIT_CASE_CONFIG.showRecommendations\" class=\"row\"><div class=\"col-xs-12\"><div rha-caserecommendations=\"\" loading=\"recommendationsLoading\"></div></div></div><div ng-show=\"EDIT_CASE_CONFIG.showComments\" class=\"row\"><div class=\"col-xs-12\"><div rha-casecomments=\"\" loading=\"loading.comments\"></div></div></div></div></div>");
}]);

angular.module("cases/views/editGroup.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/editGroup.html",
    "<div id=\"redhat-access-case\" class=\"container-offset\"><div rha-header=\"\" page=\"editGroup\" ng-controller=\"EditGroup\"></div><div class=\"container-fluid rha-side-padding\"><div style=\"padding-bottom: 20px;\" class=\"row\"><div style=\"padding-bottom: 20px;\" class=\"container-fluid\"><div class=\"col-xs-1\"><label>Group Name: </label></div><div class=\"col-xs-6\"><input type=\"text\" ng-model=\"selectedGroup.name\" ng-change=\"toggleGroupPrestine()\" class=\"form-control\"/></div></div><span ng-show=\"usersLoading\" class=\"rha-search-spinner\"></span><div ng-hide=\"usersLoading\"><div style=\"padding-bottom: 20px;\" class=\"row\"></div><div class=\"col-xs-6\"><div rha-searchbox=\"\" placeholder=\"&quot;Search Users&quot;\"></div></div><div style=\"padding-bottom: 20px;\" class=\"row\"></div><div class=\"col-xs-12\"><table ng-table=\"tableParams\" class=\"table table-bordered table-striped\"><thead style=\"text-align: center\"><th><label>Read Access</label><input type=\"checkbox\" style=\"width: 25px;\" ng-model=\"masterReadSelected\" ng-change=\"onMasterReadCheckboxClicked(masterReadSelected)\"/></th><th><label>Write Access</label><input type=\"checkbox\" style=\"width: 25px;\" ng-model=\"masterWriteSelected\" ng-change=\"onMasterWriteCheckboxClicked(masterWriteSelected)\"/></th><th ng-class=\"{&quot;sort-asc&quot;: table-params.isSortBy(&quot;sso_username&quot;, &quot;asc&quot;), &quot;sort-desc&quot;: tableParams.isSortBy(&quot;sso_username&quot;, &quot;desc&quot;)}\" ng-click=\"tableParams.sorting({&quot;sso_username&quot;: tableParams.isSortBy(&quot;sso_username&quot;, &quot;asc&quot;) ? &quot;desc&quot; : &quot;asc&quot;})\" class=\"sortable\"><div>{{'User Name'|translate}}</div></th><th ng-class=\"{&quot;sort-asc&quot;: table-params.isSortBy(&quot;first_name&quot;, &quot;asc&quot;), &quot;sort-desc&quot;: tableParams.isSortBy(&quot;first_name&quot;, &quot;desc&quot;)}\" ng-click=\"tableParams.sorting({&quot;first_name&quot;: tableParams.isSortBy(&quot;first_name&quot;, &quot;asc&quot;) ? &quot;desc&quot; : &quot;asc&quot;})\" class=\"sortable\"><div>{{'First Name'|translate}}</div></th><th ng-class=\"{&quot;sort-asc&quot;: table-params.isSortBy(&quot;last_name&quot;, &quot;asc&quot;), &quot;sort-desc&quot;: tableParams.isSortBy(&quot;last_name&quot;, &quot;desc&quot;)}\" ng-click=\"tableParams.sorting({&quot;last_name&quot;: tableParams.isSortBy(&quot;last_name&quot;, &quot;asc&quot;) ? &quot;desc&quot; : &quot;asc&quot;})\" class=\"sortable\"><div>{{'Last Name'|translate}}</div></th></thead><tbody><tr ng-repeat=\"user in usersOnScreen\"><td style=\"text-align: center; width: 25px;\"><input type=\"checkbox\" ng-disabled=\"user.write\" ng-model=\"user.access\" ng-change=\"toggleUsersPrestine()\"/></td><td style=\"text-align: center; width: 25px;\"><input type=\"checkbox\" ng-model=\"user.write\" ng-change=\"writeAccessToggle(user)\"/></td><td data-title=\"&quot;user.sso_username&quot;\" sortable=\"&quot;sso_username&quot;\">{{user.sso_username}}</td><td data-title=\"&quot;user.first_name&quot;\" sortable=\"&quot;first_name&quot;\">{{user.first_name}}</td><td data-title=\"&quot;lastName&quot;\" sortable=\"&quot;last_name&quot;\">{{user.last_name}}</td></tr></tbody></table><button ng-click=\"saveGroup()\" ng-disabled=\"isGroupPrestine &amp;&amp; isUsersPrestine\" translate=\"\" class=\"btn btn-primary\">Save Group</button><button ng-click=\"cancel()\" translate=\"\" class=\"btn btn-primary\">Cancel</button></div></div></div></div></div>");
}]);

angular.module("cases/views/emailNotifySelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/emailNotifySelect.html",
    "<h4 translate=\"\" class=\"rha-section-header\">Email Notification Recipients</h4><span ng-show=\"!securityService.loginStatus.isLoggedIn  || CaseService.usersLoading || securityService.loggingIn\" style=\"margin-left: 5px;\" class=\"rha-search-spinner\"></span><div><select chosen=\"chosen\" multiple=\"multiple\" ng-disabled=\"updatingList\" ng-model=\"CaseService.updatedNotifiedUsers\" ng-change=\"updateNotifyUsers()\" id=\"rha-email-notify-select\" width=\"&quot;100%&quot;\" ng-options=\"user.sso_username as user.sso_username for user in CaseService.users\"></select></div>");
}]);

angular.module("cases/views/entitlementSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/entitlementSelect.html",
    "<div rha-selectloadingindicator=\"\" loading=\"CaseService.entitlementsLoading\" type=\"select2\"><select id=\"rha-entitlement-select\" chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.entitlement\" ng-change=\"CaseService.onSelectChanged()\" width=\"&quot;100%&quot;\" ng-options=\"entitlement as entitlement for entitlement in CaseService.entitlements\"></select></div>");
}]);

angular.module("cases/views/exportCSVButton.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/exportCSVButton.html",
    "<button ng-click=\"exports()\" ng-hide=\"exporting || window.ie8 || window.ie9\" translate=\"\" class=\"btn btn-secondary\">Export All as CSV</button><div ng-show=\"exporting\"><span class=\"rha-search-spinner\"></span><span>{{'Exporting CSV...'|translate}}</span></div>");
}]);

angular.module("cases/views/group.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/group.html",
    "<div id=\"redhat-access-case\" class=\"container-offset\"><div rha-header=\"\" page=\"manageGroups\"></div><div ng-show=\"securityService.loginStatus.isLoggedIn\" class=\"container-fluid rha-side-padding\"><div style=\"padding-bottom: 20px;\" class=\"row\"><div class=\"col-xs-6\"><div rha-searchbox=\"\" placeholder=\"&quot;Search Groups&quot;\"></div></div><div class=\"col-xs-6\"><div rha-creategroupbutton=\"\" class=\"pull-right\"></div><div rha-deletegroupbutton=\"\" style=\"padding-right: 20px;\" class=\"pull-right\"></div><div style=\"padding-right: 20px;\" class=\"pull-right\"><button type=\"button\" translate=\"\" ng-show=\"canManageGroups\" ng-click=\"defaultCaseGroup()\" class=\"btn btn-primary\">Manage Default Case Groups</button></div></div></div><div class=\"row\"><div class=\"col-xs-12\"><div rha-grouplist=\"\"></div></div></div></div></div>");
}]);

angular.module("cases/views/groupList.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/groupList.html",
    "<span ng-show=\"groupsLoading\" class=\"rha-search-spinner\"></span><div ng-show=\"!groupsLoading &amp;&amp; listEmpty\">{{'No groups found.'|translate}}</div><div ng-hide=\"groupsLoading || listEmpty\"><table ng-table=\"tableParams\" class=\"table table-bordered table-striped\"><thead style=\"text-align: center\"><th><input type=\"checkbox\" style=\"width: 25px;\" ng-model=\"masterSelected\" ng-change=\"onMasterCheckboxClicked()\"/></th><th ng-class=\"{&quot;sort-asc&quot;: table-params.isSortBy(&quot;name&quot;, &quot;asc&quot;), &quot;sort-desc&quot;: tableParams.isSortBy(&quot;name&quot;, &quot;desc&quot;)}\" ng-click=\"tableParams.sorting({&quot;name&quot;: tableParams.isSortBy(&quot;name&quot;, &quot;asc&quot;) ? &quot;desc&quot; : &quot;asc&quot;})\" class=\"sortable\"><div>{{'Name'|translate}}</div></th></thead><tbody><tr ng-repeat=\"group in GroupService.groupsOnScreen\"><td style=\"text-align: center; width: 25px;\"><input type=\"checkbox\" ng-model=\"group.selected\" ng-change=\"onGroupSelected()\"/></td><td data-title=\"&quot;Group Name&quot;\" sortable=\"&quot;name&quot;\"><a ng-show=\"canManageGroups\" ng-href=\"#/case/group/{{group.number}}\">{{group.name}}</a><p ng-hide=\"canManageGroups\">{{group.name}}</p></td></tr></tbody></table></div>");
}]);

angular.module("cases/views/groupSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/groupSelect.html",
    "<div rha-selectloadingindicator=\"\" loading=\"CaseService.groupsLoading\" type=\"select2\" class=\"group-select\"><select id=\"rha-group-select\" chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.group\" ng-change=\"CaseService.onGroupSelectChanged()\" placeholder=\"Select a Group\" width=\"&quot;100%&quot;\" ng-options=\"option.value as option.label for option in CaseService.groupOptions\" options-disabled=\"option.isDisabled for option in CaseService.groupOptions\"></select></div>");
}]);

angular.module("cases/views/list.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/list.html",
    "<div id=\"redhat-access-case\" style=\"padding-bottom: 15px;\" class=\"container-offset\"><div rha-header=\"\" page=\"caseList\"></div><div ng-show=\"!securityService.loginStatus.userAllowedToManageCases &amp;&amp; securityService.loginStatus.isLoggedIn\" class=\"row\"><div>{{'User does not have permissions to manage cases.'|translate}}</div></div><div ng-hide=\"!securityService.loginStatus.userAllowedToManageCases &amp;&amp; securityService.loginStatus.isLoggedIn\"><div class=\"container-fluid rha-side-padding\"><div class=\"row\"><div class=\"col-md-8\"><div rha-listfilter=\"\"></div></div><div class=\"col-md-4 text-right\"><span rha-chatbutton=\"\" class=\"pad-r-l\"></span><button ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ui-sref=\"new\" translate=\"\" class=\"btn btn-primary btn-slim btn-sm\">Open a New Support Case</button></div></div></div><div style=\"margin-left: 10px; margin-right: 10px;\" class=\"rha-bottom-border\"></div><div class=\"container-fluid rha-side-padding\"><div ng-show=\"SearchCaseService.searching &amp;&amp; securityService.loginStatus.isLoggedIn\" class=\"row\"><div class=\"col-xs-12\"><span class=\"rha-search-spinner\"></span></div></div><div ng-show=\"SearchCaseService.cases.length == 0 &amp;&amp; !SearchCaseService.searching &amp;&amp; securityService.loginStatus.isLoggedIn\" class=\"row\"><div class=\"col-xs-12\"><div>{{'No cases found with given filters.'|translate}}</div></div></div><div ng-hide=\"SearchCaseService.cases.length == 0 || SearchCaseService.searching || !securityService.loginStatus.isLoggedIn\"><div class=\"row\"><div class=\"col-xs-12\"><table ng-table=\"tableParams\" style=\"text-align: left\" class=\"table table-bordered table-striped\"><tr ng-repeat=\"case in $data\"><td data-title=\"&quot;Case ID&quot;\" sortable=\"&quot;case_number&quot;\" style=\"width: 10%\"><a href=\"#/case/{{case.case_number}}\">{{case.case_number}}</a></td><td data-title=\"&quot;Summary&quot;\" sortable=\"&quot;summary&quot;\" style=\"width: 15%\">{{case.summary}}</td><td data-title=\"&quot;Product/Version&quot;\" sortable=\"&quot;product&quot;\">{{case.product}} / {{case.version}}</td><td data-title=\"&quot;Status&quot;\" sortable=\"&quot;status&quot;\">{{case.status}}</td><td data-title=\"&quot;Severity&quot;\" sortable=\"&quot;severity&quot;\">{{case.severity}}</td><td data-title=\"&quot;Owner&quot;\" sortable=\"&quot;owner&quot;\">{{case.contact_name}}</td><td data-title=\"&quot;Opened&quot;\" sortable=\"&quot;created_date&quot;\" style=\"width: 10%\">{{case.created_date | date:'longDate'}}</td><td data-title=\"&quot;Updated&quot;\" sortable=\"&quot;last_modified_date&quot;\" style=\"width: 10%\">{{case.last_modified_date | date:'longDate'}}</td></tr></table></div></div><div class=\"row\"><div class=\"col-xs-12\"><div rha-exportcsvbutton=\"\"></div></div></div></div></div></div></div>");
}]);

angular.module("cases/views/listAttachments.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/listAttachments.html",
    "<div ng-show=\"AttachmentsService.originalAttachments.length == 0\" style=\"padding-bottom: 10px;\" class=\"rha-attachments-section\">{{'No attachments added'|translate}}</div><div ng-show=\"AttachmentsService.originalAttachments.length &gt; 0\" class=\"panel panel-default\"><div class=\"panel-heading\">{{'Attached Files'|translate}}</div><table class=\"table table-hover table-bordered\"><thead><th>{{'Filename'|translate}}</th><th>{{'Description'|translate}}</th><th>{{'Size'|translate}}</th><th>{{'Attached'|translate}}</th><th>{{'Attached By'|translate}}</th><th>{{'Delete'|translate}}</th></thead><tbody><tr ng-repeat=\"attachment in AttachmentsService.originalAttachments\"><td><a ng-hide=\"attachment.uri == null\" href=\"{{attachment.uri}}\">{{attachment.file_name}}</a><div ng-show=\"attachment.uri == null\">{{attachment.file_name}}</div></td><td>{{attachment.description}}</td><td>{{attachment.length | bytes}}</td><td>{{attachment.created_date | date:'medium'}}</td><td>{{attachment.created_by}}</td><td><div ng-show=\"disabled\">{{'Delete'|translate}}</div><a ng-click=\"AttachmentsService.removeOriginalAttachment($index)\" ng-hide=\"disabled\">{{'Delete'|translate}}</a></td></tr></tbody></table></div>");
}]);

angular.module("cases/views/listBugzillas.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/listBugzillas.html",
    "<div ng-show=\"securityService.loginStatus.authedUser.is_internal\" class=\"redhat-access-bz\"><h4 style=\"padding-top: 20px;\" class=\"rha-section-header\">{{'Bugzilla Tickets'|translate}}</h4><span ng-show=\"loading\" class=\"rha-search-spinner\"></span><div ng-hide=\"CaseService.bugzillaList.bugzilla.length &gt; 0\" style=\"padding-bottom: 10px;\">{{'No linked bugzillas'|translate}}</div><div ng-show=\"CaseService.bugzillaList.bugzilla.length &gt; 0\" class=\"panel panel-default\"><table class=\"table table-hover table-bordered\"><thead><th>{{'Bugzilla Number'|translate}}</th><th>{{'Summary of Request'|translate}}</th></thead><tbody>  <tr ng-repeat=\"bugzilla in CaseService.bugzillaList.bugzilla\"><td><a href=\"{{bugzilla.resource_view_uri}}\" target=\"_blank\">{{bugzilla.bugzilla_number}}</a></td><td>{{bugzilla.summary}}</td></tr></tbody></table></div></div>");
}]);

angular.module("cases/views/listFilter.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/listFilter.html",
    "<div class=\"container-fluid\"><div class=\"row\"><div class=\"col-md-6 pad-b-l\"><div rha-searchbox=\"\" placeholder=\"Search\"></div></div><div class=\"col-md-3 pad-s-y\"><div><span popover=\"Filtering by case groups helps you find related cases.\" tabindex=\"0\" popover-append-to-body=\"true\" popover-trigger=\"mouseenter\" class=\"glyphicon glyphicon-question-sign pull-right\"></span><div rha-groupselect=\"\" ng-init=\"setSearchOptions(true)\"></div></div></div><div class=\"col-md-3 pad-s-y\"><div rha-statusselect=\"\"></div></div></div></div>");
}]);

angular.module("cases/views/listNewAttachments.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/listNewAttachments.html",
    "<div class=\"container-fluid\"><div class=\"row\"><div class=\"col-xs-12\"><div class=\"panel panel-default\"><div class=\"panel-heading\">{{'Files to Attach'|translate}}</div><ul class=\"list-group\"><li ng-repeat=\"attachment in AttachmentsService.updatedAttachments\" ng-hide=\"AttachmentsService.updatedAttachments.length &lt;= 0\" class=\"list-group-item\">{{attachment.file_name}} ({{attachment.length | bytes}}) - {{attachment.description}}<button type=\"button\" style=\"float: right\" ng-click=\"removeLocalAttachment($index)\" class=\"close\">&times;</button></li><li ng-repeat=\"attachment in TreeViewSelectorUtils.getSelectedLeaves(AttachmentsService.backendAttachments)\" ng-hide=\"TreeViewSelectorUtils.getSelectedLeaves(AttachmentsService.backendAttachments).length &lt;= 0\" class=\"list-group-item\">{{attachment}}</li></ul></div></div></div></div>");
}]);

angular.module("cases/views/new.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/new.html",
    "<div class=\"container-offset\"><div id=\"redhat-access-case\" class=\"container-fluid\"><div rha-header=\"\" page=\"newCase\"></div><div ng-show=\"securityService.loginStatus.isLoggedIn &amp;&amp; securityService.loginStatus.authedUser.has_chat\" class=\"row\"><div class=\"pull-right\"><div rha-chatbutton=\"\" style=\"margin-right: 10px;\"></div></div></div><div ng-show=\"securityService.loginStatus.isLoggedIn &amp;&amp; securityService.loginStatus.authedUser.has_chat\" class=\"rha-bottom-border\"></div><div ng-hide=\"!NEW_CASE_CONFIG.isPCM\" class=\"container-fluid row\"><div class=\"full-border col-md-12\"><div class=\"col-md-4 center\"><label>{{'Product & Topic'|translate}}</label><div class=\"col-md-12\"><div ng-attr-class=\"{{isPage1 &amp;&amp; 'no-fun' || 'fun' }}\"></div></div></div><div class=\"col-md-4 center\"><label>{{'Case Details'|translate}}</label><div class=\"col-md-12\"><div ng-attr-class=\"{{(isPage2 &amp;&amp; !submittingCase) &amp;&amp; 'no-fun' || 'fun' }}\"></div></div></div><div class=\"col-md-4 center\"><label>{{'Creating Case'|translate}}</label><div class=\"col-md-12\"><div ng-attr-class=\"{{submittingCase &amp;&amp; 'no-fun' || 'fun' }}\"></div></div></div></div></div><div ng-class=\"{'partial-border': NEW_CASE_CONFIG.isPCM}\" class=\"container-fluid rha-side-padding\"><div style=\"border-right: 1px solid; border-color: #cccccc;\" class=\"col-xs-6\"><div class=\"container-fluid rha-side-padding\"><div ng-class=\"{&quot;hidden&quot;: isPage2}\" id=\"rha-case-wizard-page-1\" class=\"rha-create-case-section\"><div ng-if=\"securityService.loginStatus.authedUser.is_internal\"><div class=\"row rha-create-field\"><div class=\"col-md-3\"><label for=\"rha-account-number\">{{'Account:'|translate}}</label></div><div class=\"col-md-9\"><div rha-accountselect=\"\"></div></div></div><div class=\"row rha-create-field\"><div class=\"col-md-3\"><label for=\"rha-owners-select\">{{'Owner:'|translate}}</label></div><div class=\"col-md-9\"><div rha-ownerselect=\"\"></div></div></div></div><div class=\"row rha-create-field\"><div class=\"col-md-3\"><label for=\"rha-product-select\">{{'Product:'|translate}}</label></div><div class=\"col-md-9\"><div rha-selectloadingindicator=\"\" loading=\"productsLoading\" type=\"bootstrap\"><select id=\"rha-product-select\" ng-disabled=\"!securityService.loginStatus.isLoggedIn || submittingCase\" style=\"width: 100%;\" ng-model=\"CaseService.kase.product\" ng-change=\"getProductVersions(CaseService.kase.product);getRecommendations()\" ng-options=\"p.value as p.label for p in products\" options-disabled=\"p.isDisabled for p in products\" class=\"form-control\"></select></div></div></div><div class=\"row rha-create-field\"><div class=\"col-md-3\"><label for=\"rha-product-version-select\">{{'Product Version:'|translate}}</label></div><div class=\"col-md-9\"><div><div rha-selectloadingindicator=\"\" loading=\"versionLoading\" type=\"bootstrap\"><select id=\"rha-product-version-select\" style=\"width: 100%;\" ng-model=\"CaseService.kase.version\" ng-options=\"v for v in versions\" ng-change=\"CaseService.validateNewCasePage1();getRecommendations()\" ng-disabled=\"versionDisabled || !securityService.loginStatus.isLoggedIn || submittingCase\" class=\"form-control\"></select></div><div ng-show=\"CaseService.showVersionSunset()\" class=\"versionSunsetMessage\"><span>{{'This release is now retired, please refer to the recommended FAQ prior to filing a case'|translate}}</span></div></div></div></div><div class=\"row rha-create-field\"><div class=\"col-md-3\"><label for=\"rha-case-summary\">{{'Summary:'|translate}}</label></div><div class=\"col-md-9\"><input id=\"rha-case-summary\" style=\"width: 100%;\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-change=\"CaseService.validateNewCasePage1()\" ng-model=\"CaseService.kase.summary\" class=\"form-control\"/></div></div><div class=\"row rha-create-field\"><div class=\"col-md-3\"><label for=\"rha-case-description\">{{'Description:'|translate}}</label></div><div class=\"col-md-9\"><textarea id=\"rha-case-description\" style=\"width: 100%; height: 200px; max-width: 100%;\" ng-model=\"CaseService.kase.description\" ng-change=\"CaseService.validateNewCasePage1()\" ng-disabled=\"!securityService.loginStatus.isLoggedIn || submittingCase\" class=\"form-control description-box\"></textarea></div></div><div class=\"row\"><div ng-class=\"{&quot;hidden&quot;: isPage2}\" class=\"col-xs-12\"><button style=\"float: right\" ng-click=\"doNext()\" ng-disabled=\"CaseService.newCasePage1Incomplete\" translate=\"\" class=\"btn btn-primary btn-next\">Next</button></div></div></div><div ng-class=\"{hidden: isPage1}\" id=\"rha-case-wizard-page-2\" class=\"rha-create-case-section\"><div class=\"rha-bottom-border\"><div class=\"row\"><div class=\"col-xs-12\"><div style=\"margin-bottom: 10px;\" class=\"rha-bold\">{{CaseService.kase.product.name}} {{CaseService.kase.version}}</div></div></div><div class=\"row\"><div class=\"col-xs-12\"><div style=\"font-size: 90%; margin-bottom: 4px;\" class=\"rha-bold\">{{CaseService.kase.summary}}</div></div></div><div class=\"row\"><div class=\"col-xs-12\"><div style=\"font-size: 85%\">{{CaseService.kase.description}}</div></div></div></div><div class=\"row rha-create-field\"><div ng-hide=\"CaseService.entitlements.length &lt;= 1\" class=\"col-md-4\"><label for=\"rha-entitlement-select\">Support Level:</label></div><div ng-show=\"CaseService.entitlements.length &lt;= 1\" class=\"col-md-8\">{{CaseService.entitlements[0]}}</div><div ng-hide=\"CaseService.entitlements.length &lt;= 1\" class=\"col-md-8\"><div rha-entitlementselect=\"\"></div></div></div><div class=\"row rha-create-field\"><div class=\"col-md-4\"><label for=\"rha-severity\">{{'Severity:'|translate}}</label></div><div class=\"col-md-8\"><div rha-loadingindicator=\"\" loading=\"severitiesLoading\"><select id=\"rha-severity\" style=\"width: 100%;\" ng-model=\"CaseService.kase.severity\" ng-change=\"validatePage2()\" ng-disabled=\"submittingCase\" ng-options=\"s.name for s in CaseService.severities track by s.name\" class=\"form-control\"></select></div></div></div><div ng-show=\"CaseService.showFts()\" style=\"padding-left: 30px;\"><div class=\"row rha-create-field\"><div class=\"col-md-12\"><span>{{'24x7 Support:'|translate}}</span><input type=\"checkbox\" ng-model=\"CaseService.fts\" style=\"display: inline-block; padding-left: 10px;\"/></div></div><div ng-show=\"CaseService.fts\" class=\"row rha-create-field\"><div class=\"col-md-4\"><div>{{'24x7 Contact:'|translate}}</div></div><div class=\"col-md-8\"><input ng-model=\"CaseService.fts_contact\" class=\"form-control\"/></div></div></div><div class=\"row rha-create-field\"><div class=\"col-md-4\"><label for=\"rha-group-select\">{{'Case Group:'|translate}}</label></div><div class=\"col-md-8\"><div rha-groupselect=\"\" ng-init=\"setSearchOptions('false')\"></div></div></div><div ng-show=\"NEW_CASE_CONFIG.showAttachments &amp;&amp; securityService.loginStatus.authedUser.can_add_attachments\"><div class=\"row rha-create-field\"><div class=\"col-xs-12\"><label>{{'Attachments:'|translate}}</label></div></div><div class=\"rha-bottom-border\"><div style=\"overflow: auto\" class=\"row rha-create-field\"><div class=\"col-xs-12\"><div rha-listnewattachments=\"\"></div></div></div><div ng-hide=\"submittingCase\" class=\"row rha-create-field\"><div class=\"col-xs-12\"><div rha-attachlocalfile=\"\" disabled=\"submittingCase\"></div></div></div><div ng-hide=\"submittingCase\" class=\"row rha-create-field\"><div class=\"col-xs-12\"><div ng-show=\"NEW_CASE_CONFIG.showServerSideAttachments\"><div class=\"server-attach-header\">Server File(s) To Attach:<div rha-choicetree=\"\" ng-model=\"attachmentTree\" ng-controller=\"BackEndAttachmentsCtrl\"></div></div></div></div></div></div></div><div style=\"margin-top: 20px;\" class=\"row\"><div class=\"col-xs-6\"><button style=\"float: left\" ng-click=\"doPrevious()\" ng-disabled=\"submittingCase\" translate=\"\" class=\"btn btn-primary btn-previous\">Previous</button></div><div class=\"col-xs-6\"><button style=\"float: right\" ng-disabled=\"submittingCase\" ng-hide=\"submittingCase\" ng-click=\"doSubmit($event)\" translate=\"\" class=\"btn btn-primary btn-submit\">Submit</button><span ng-show=\"submittingCase\" style=\"float: right\" class=\"rha-search-spinner\"></span></div></div></div></div></div><div style=\"overflow: auto;\" ng-show=\"NEW_CASE_CONFIG.showRecommendations\" class=\"col-xs-6\"><div ng-controller=\"SearchController\" style=\"overflow: vertical;\"><div ng-hide=\"!NEW_CASE_CONFIG.isPCM\" rha-newrecommendations=\"\"></div><div ng-hide=\"NEW_CASE_CONFIG.isPCM\" rha-accordionsearchresults=\"\"></div></div></div></div></div></div>");
}]);

angular.module("cases/views/newRecommendationsSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/newRecommendationsSection.html",
    "<div id=\"rha-new-recommendation-section\" class=\"row rha-bottom-border\"><div class=\"col-xs-12\"><div style=\"padding-bottom: 0\"><span><h4 style=\"padding-left: 10px; display: inline-block;\">Red Hat Access Recommendations</h4></span><span ng-show=\"searchInProgress.value\" class=\"rha-search-spinner\"></span></div></div></div><div class=\"col-xs-12\"><div class=\"recommendations-inner\"><ul style=\"display: block;\" class=\"recommendations\"><li ng-repeat=\"result in results\"><h4><span class=\"icon-solution\"></span><a ng-click=\"triggerAnalytics($event)\" href=\"{{result.view_uri}}\" target=\"_blank\">{{result.title}} </a></h4><p class=\"snippet\">{{result.resolution.text | recommendationsResolution}}</p><div class=\"row\"><div ng-repeat=\"product in result.products.product\"><div class=\"col-xs-3\"><span class=\"recommendations_products\">{{product}}</span></div></div><div ng-repeat=\"tag in result.tags.tag\"><div class=\"col-xs-3\"></div></div><span class=\"recommendations_tags\">{{tag}}</span></div></li></ul><div style=\"padding-top: 10px;\" ng-hide=\"results.length == 0\" class=\"row\"><div class=\"col-xs-12\"><pagination boundary-links=\"true\" total-items=\"SearchResultsService.results.length\" on-select-page=\"selectPage(page)\" items-per-page=\"itemsPerPage\" page=\"currentPage\" rotate=\"false\" max-size=\"maxPagerSize\" previous-text=\"&lt;\" next-text=\"&gt;\" first-text=\"&lt;&lt;\" last-text=\"&gt;&gt;\" class=\"pagination-sm\"></pagination></div></div></div></div>");
}]);

angular.module("cases/views/ownerSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/ownerSelect.html",
    "<div><div rha-selectloadingindicator=\"\" loading=\"CaseService.usersLoading\" type=\"select2\"><select id=\"rha-owner-select\" chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.owner\" ng-change=\"CaseService.onOwnerSelectChanged()\" ng-options=\"user.sso_username as (user.first_name + &quot; &quot; + user.last_name + &quot; &lt;&quot; + user.sso_username + &quot;&gt;&quot;) for user in CaseService.users\" width=\"&quot;100%&quot;\"></select></div></div>");
}]);

angular.module("cases/views/productSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/productSelect.html",
    "<div rha-selectloadingindicator=\"\" loading=\"productsLoading\" type=\"select2\"><select chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.product\" ng-change=\"CaseService.onSelectChanged()\" width=\"&quot;100%&quot;\" ng-options=\"product.name as product.name for product in CaseService.products\"></select></div>");
}]);

angular.module("cases/views/recommendationsSection.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/recommendationsSection.html",
    "<div id=\"rha-recommendation-section\"><div style=\"margin-bottom: 10px;\" class=\"rha-section-header\"><h4 style=\"display: inline; padding-right: 10px;\" translate=\"\">Recommendations</h4><span style=\"display: inline-block; height: 11px; width: 11px;\" ng-show=\"RecommendationsService.loadingRecommendations\" class=\"rha-search-spinner-sm\"></span></div><span ng-show=\"loading\" class=\"rha-search-spinner\"></span><div ng-hide=\"loading\" class=\"container-fluid rha-side-padding\"><div class=\"row\"><div ng-repeat=\"recommendation in RecommendationsService.recommendationsOnScreen\"><div class=\"col-xs-3\"><div style=\"position: absolute; left: 0px;\"><span ng-class=\"{pinned: recommendation.pinned &amp;&amp; !recommendation.pinning, &quot;not-pinned&quot;: !recommendation.pinned &amp;&amp; !recommendation.pinning, &quot;rha-search-spinner-sm&quot;: recommendation.pinning}\" ng-click=\"pinRecommendation(recommendation, $index, $event)\" style=\"cursor: pointer;\">&nbsp;</span></div><div><div style=\"overflow: hidden;\" class=\"rha-bold\">{{recommendation.title}}</div><span ng-show=\"recommendation.handPicked\" ng-class=\"{&quot;hand-picked&quot;: recommendation.handPicked}\" translate=\"\">handpicked</span><div style=\"padding: 8px 0;word-wrap:break-word;\">{{recommendation.resolution.text | recommendationsResolution}}</div><a ng-click=\"triggerAnalytics($event)\" href=\"{{recommendation.view_uri}}\" target=\"_blank\">{{'View full article in new window'|translate}}</a></div></div></div></div><div style=\"padding-top: 10px;\" ng-hide=\"RecommendationsService.recommendationsOnScreen.length == 0\" class=\"row\"><div class=\"col-xs-12\"><pagination boundary-links=\"true\" total-items=\"RecommendationsService.recommendations.length\" on-select-page=\"RecommendationsService.selectPage(page)\" items-per-page=\"RecommendationsService.pageSize\" page=\"RecommendationsService.currentPage\" max-size=\"RecommendationsService.maxSize\" previous-text=\"&lt;\" next-text=\"&gt;\" first-text=\"&lt;&lt;\" last-text=\"&gt;&gt;\" class=\"pagination-sm\"></pagination></div></div></div></div>");
}]);

angular.module("cases/views/requestManagementEscalationModal.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/requestManagementEscalationModal.html",
    "<div class=\"modal-header\"><h3 translate=\"\">Request Management Escalation</h3></div><div style=\"padding: 20px;\" class=\"container-fluid\"><div class=\"row\"><div class=\"col-sm-12\"><span>{{'If you feel the issue has become more severe or the case should be a higher priority, please provide a detailed comment, and the case will be reviewed by a support manager.'|translate}}</span><a href=\"https://access.redhat.com/site/support/policy/mgt_escalation\" target=\"_blank\">{{'Learn more'|translate}}</a></div></div><div style=\"padding-top: 10px;\" class=\"row\"><div class=\"col-sm-12\"><div>{{'Comment:'|translate}}</div><textarea style=\"width: 100%; max-width: 100%; height: 200px;\" ng-model=\"CaseService.escalationCommentText\" ng-disabled=\"submittingRequest\" ng-change=\"onNewEscalationComment()\"></textarea></div></div><div style=\"border-top: 1px; solid #cccccc; padding-top: 10px;\" class=\"row\"><div class=\"col-sm-12\"><div class=\"pull-right\"><button id=\"rha-case-escalation-submitbutton\" style=\"margin-left: 10px;\" ng-click=\"submitRequestClick(CaseService.escalationCommentText)\" ng-disabled=\"submittingRequest || disableSubmitRequest\" class=\"btn-secondary btn\"><span>{{'Submit Request'|translate}}</span></button></div><button ng-click=\"closeModal()\" ng-disabled=\"submittingRequest\" class=\"btn-secondary btn pull-right\">Cancel</button><span ng-show=\"submittingRequest\" class=\"rha-search-spinner\"></span></div></div></div>");
}]);

angular.module("cases/views/search.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/search.html",
    "<div id=\"rha-case-search\" class=\"container-offset\"><div rha-header=\"\" page=\"searchCase\"></div><div class=\"container-fluid\"><div style=\"padding-bottom: 10px;\" class=\"row\"><div class=\"col-xs-6\"><div rha-searchbox=\"\" placeholder=\"Search\"></div></div><div class=\"col-xs-3\"><button ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ui-sref=\"new\" translate=\"\" class=\"btn btn-secondary pull-right\">Open a New Support Case</button></div></div><div class=\"rha-bottom-border\"></div><div style=\"padding-bottom: 10px;\" class=\"row\"><div class=\"col-sm-2\"><label translate=\"\">Status</label><div rha-statusselect=\"\"></div></div><div class=\"col-sm-2\"><label translate=\"\">Severity</label><div rha-severityselect=\"\"></div></div><div class=\"col-sm-2\"><label translate=\"\">Type</label><div rha-typeselect=\"\"></div></div><div class=\"col-sm-2\"><label translate=\"\">Group</label><div rha-groupselect=\"\" show-search-options=\"true\"></div></div><div class=\"col-sm-2\"><label translate=\"\">Owner</label><div rha-ownerselect=\"\"></div></div><div class=\"col-sm-2\"><label translate=\"\">Product</label><div rha-productselect=\"\"></div></div></div><div ng-show=\"SearchCaseService.searching &amp;&amp; securityService.loginStatus.isLoggedIn\"><div style=\"padding-bottom: 4px;\" class=\"row\"><div class=\"col-xs-12\"><span class=\"rha-search-spinner\"></span><h3 style=\"display: inline-block; padding-left: 4px;\" translate=\"\">Searching...</h3></div></div></div><div ng-show=\"SearchCaseService.cases.length === 0 &amp;&amp; !SearchCaseService.searching &amp;&amp; securityService.loginStatus.isLoggedIn\"><div class=\"row\"><div class=\"col-xs-12\"><div>{{'No cases found with given search criteria.'|translate}}</div></div></div></div><div ng-repeat=\"case in casesOnScreen\"><div class=\"row\"><div class=\"col-xs-12\"><div rha-casesearchresult=\"\" case=\"case\"></div></div></div></div><div ng-hide=\"SearchCaseService.cases.length === 0\" style=\"border-top: 1px solid #cccccc\"><div class=\"row\"><div class=\"col-xs-6 pull-right\"><pagination style=\"float: right; cursor: pointer;\" boundary-links=\"false\" total-items=\"SearchCaseService.cases.length\" on-select-page=\"selectPage(page)\" items-per-page=\"itemsPerPage\" page=\"currentPage\" max-size=\"maxPagerSize\" rotate=\"true\" class=\"pagination-sm\"></pagination></div><div style=\"padding-top: 20px;\" class=\"col-xs-6 pull-left\"><div rha-exportcsvbutton=\"\"></div></div></div></div></div></div>");
}]);

angular.module("cases/views/searchBox.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/searchBox.html",
    "<div class=\"input-group\"><input id=\"rha-searchform-searchbox\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" placeholder=\"{{placeholder}}\" ng-model=\"SearchBoxService.searchTerm\" ng-keypress=\"onFilterKeyPress($event)\" ng-change=\"SearchBoxService.onChange()\" class=\"form-control\"/><span class=\"input-group-btn\"><button id=\"rha-searchform-searchbutton\" ng-click=\"SearchBoxService.doSearch()\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" class=\"btn btn-default btn-primary\"><i class=\"glyphicon glyphicon-search\"></i> Search</button></span></div>");
}]);

angular.module("cases/views/searchResult.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/searchResult.html",
    "<div id=\"rha-case-search-result\"><div ng-class=\"{hascomment: theCase.comments.comment !== undefined}\" style=\"padding-top: 10px; border-top: 1px solid #cccccc; display: table; width: 100%;\" class=\"hover\"><span style=\"display: table-cell; vertical-align: top; width: 20px;\" class=\"glyphicon glyphicon-briefcase\"></span><div style=\"display: table-cell; padding-right: 20px;\"><div class=\"container-fluid\"><div style=\"padding-bottom: 6px;\" class=\"row\"><div class=\"col-xs-12\"><div style=\"display: inline-block; font-weight: bold;\"><a ng-href=\"#/case/{{theCase.case_number}}\">{{theCase.case_number}} - {{theCase.summary}}</a></div></div></div><div style=\"padding-bottom: 10px;\" class=\"row\"><div class=\"col-xs-4\"><span style=\"padding-right: 4px;\" class=\"detail-name\">{{'Updated:'|translate}}</span><span class=\"detail-value\">{{theCase.last_modified_date | date: 'medium'}}</span></div><div class=\"col-xs-4\"><span style=\"padding-right: 4px;\" class=\"detail-name\">{{'Status:'|translate}}</span><span ng-class=\"{closed: theCase.status === &quot;Closed&quot;, redhat: theCase.status === &quot;Waiting on Red Hat&quot;, customer: theCase.status === &quot;Waiting on Customer&quot;}\" class=\"detail-value status\">{{theCase.status}}</span></div><div class=\"col-xs-4\"><span style=\"padding-right: 4px;\" class=\"detail-name\">{{'Severity:'|translate}}</span><span class=\"detail-value\">{{theCase.severity}}</span></div></div><div ng-show=\"theCase.comments.comment !== undefined\" class=\"row\"><div class=\"col-xs-12\"><div class=\"well\">{{theCase.comments.comment[0].text}}</div><span class=\"comment-tip\"></span><div class=\"comment-user\"><span class=\"avatar\"></span><div class=\"commenter\">{{theCase.comments.comment[0].created_by}}</div><div class=\"comment-date\">{{theCase.comments.comment[0].created_date | date: 'medium'}}</div></div></div></div></div></div></div></div>");
}]);

angular.module("cases/views/selectLoadingIndicator.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/selectLoadingIndicator.html",
    "<div id=\"rha-select-loading-indicator\"><progressbar ng-show=\"loading\" max=\"1\" value=\"1\" animate=\"false\" ng-class=\"{select2: type === &quot;select2&quot;, bootstrap: type === &quot;bootstrap&quot;}\" style=\"margin-bottom: 0px;\" class=\"progress-striped active\"></progressbar><div ng-transclude=\"ng-transclude\" ng-hide=\"loading\"></div></div>");
}]);

angular.module("cases/views/severitySelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/severitySelect.html",
    "<div rha-selectloadingindicator=\"\" loading=\"severitiesLoading\" type=\"select2\"><select chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.severity\" ng-change=\"CaseService.onSelectChanged()\" width=\"&quot;100%&quot;\" ng-options=\"severity.name as severity.name for severity in CaseService.severities\"></select></div>");
}]);

angular.module("cases/views/statusSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/statusSelect.html",
    "<div style=\"display: block\"><select id=\"rha-status-select\" chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.status\" ng-change=\"CaseService.onSelectChanged()\" width=\"&quot;100%&quot;\" ng-options=\"status.value as status.name for status in statuses\"></select></div>");
}]);

angular.module("cases/views/typeSelect.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("cases/views/typeSelect.html",
    "<div rha-selectloadingindicator=\"\" loading=\"typesLoading\" type=\"select2\"><select chosen=\"chosen\" ng-disabled=\"!securityService.loginStatus.isLoggedIn\" ng-model=\"CaseService.type\" ng-change=\"CaseService.onSelectChanged()\" width=\"&quot;100%&quot;\" ng-options=\"type.name as type.name for type in CaseService.types\"></select></div>");
}]);

angular.module("log_viewer/views/logTabs.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("log_viewer/views/logTabs.html",
    "<div tabset ng-show='tabs.length > 0'>\n" +
    "    <div tab active=\"tab.active\" ng-repeat=\"tab in tabs\">\n" +
    "        <div tab-heading>{{tab.shortTitle}}\n" +
    "            <a ng-click=\"removeTab($index)\" href=''>\n" +
    "                <span class=\"glyphicon glyphicon-remove\"></span>\n" +
    "            </a>\n" +
    "        </div>\n" +
    "        <div class=\"panel panel-default\">\n" +
    "            <div class=\"panel-heading\">\n" +
    "                <a popover=\"Click to refresh log file.\" popover-trigger=\"mouseenter\" popover-placement=\"right\" ng-click=\"refreshTab($index)\">\n" +
    "                    <span class=\"glyphicon glyphicon-refresh\"></span>\n" +
    "                </a>\n" +
    "                <h3 class=\"panel-title\" style=\"display: inline\">{{tab.longTitle}}</h3>\n" +
    "                <div class=\"pull-right\" id=\"overlay\" popover=\"Select text and click to perform Red Hat Diagnose\" popover-trigger=\"mouseenter\" popover-placement=\"left\">\n" +
    "                    <button ng-disabled=\"isDisabled\" id=\"diagnoseButton\" type=\"button\" class=\"btn btn-sm btn-primary diagnoseButton\" ng-click=\"diagnoseText()\" translate='' >Red Hat Diagnose</button>\n" +
    "                </div>\n" +
    "                <a class=\"tabs-spinner\" ng-class=\"{ showMe: isLoading }\">\n" +
    "                    <span class=\"rha-search-spinner\"></span>\n" +
    "                </a>\n" +
    "\n" +
    "                <br>\n" +
    "                <br>\n" +
    "            </div>\n" +
    "            <div class=\"panel-body\" rha-filldown ng-style=\"{ height: windowHeight }\">\n" +
    "\n" +
    "                <pre id=\"resizeable-file-view\" class=\"no-line-wrap\">{{tab.content}}</pre>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>\n" +
    "");
}]);

angular.module("log_viewer/views/log_viewer.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("log_viewer/views/log_viewer.html",
    "<div id=\"log_view_main\" style=\"max-height: 500px;\">\n" +
    "    <div class=\"container-offset\">\n" +
    "        <div rha-header page=\"logViewer\"></div>\n" +
    "    </div>\n" +
    "    <div class=\"row-fluid\" ng-controller=\"logViewerController\" ng-mouseup=\"enableDiagnoseButton()\">\n" +
    "        <div rha-navsidebar></div>\n" +
    "        <div class=col-fluid>\n" +
    "            <div rha-recommendations></div>\n" +
    "            <div class=\"col-fluid\">\n" +
    "                <div ng-controller=\"TabsDemoCtrl\" ng-class=\"{ showMe: solutionsToggle }\">\n" +
    "                    <div rha-logsinstructionpane></div>\n" +
    "                    <div rha-logtabs></div>\n" +
    "                </div>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "</div>");
}]);

angular.module("log_viewer/views/logsInstructionPane.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("log_viewer/views/logsInstructionPane.html",
    "<div class=\"panel panel-default rha-logsinstructionpane\" ng-hide=\"tabs.length > 0\" rha-filldown ng-style=\"{ height: windowHeight }\" style=\"overflow:auto\">\n" +
    "                        <div class=\"panel-body\" >\n" +
    "                            <div>\n" +
    "                                <h2 translate=''>Log File Viewer</h2>\n" +
    "                                <p>\n" +
    "                                    <h3 translate=''>The log file viewer gives the ability to diagnose application logs as well as file a support case with Red Hat Global Support Services.\n" +
    "                                    </h3>\n" +
    "                            </div>\n" +
    "                            <div>\n" +
    "                                <br>\n" +
    "                                <h4 translate>\n" +
    "                                    <span class=\"glyphicon glyphicon-refresh\"></span>&nbsp;Select Log</h4>\n" +
    "                                <p translate>\n" +
    "                                    Simply navigate to and select a log file from the list on the left and click the 'Select File' button. </p>\n" +
    "\n" +
    "                            </div>\n" +
    "                            <div>\n" +
    "                                <br>\n" +
    "                                <h4>\n" +
    "                                    <span class=\"glyphicon glyphicon-search\"></span>&nbsp;{{'Diagnose'|translate}}\n" +
    "                                </h4>\n" +
    "                                <p translate>Once you have selected your log file then you may diagnose any part of the log file and clicking the 'Red Hat Diagnose' button. This will then display relevant articles and solutons from our Red Hat Knowledge base.</p>\n" +
    "\n" +
    "                            </div>\n" +
    "                            <div>\n" +
    "                                <br>\n" +
    "                                <h4 translate>\n" +
    "                                    <span class=\"glyphicon glyphicon-plus\"></span>&nbsp;Open a New Support Case\n" +
    "                                </h4>\n" +
    "                                <p translate>In the event that you would still like to open a support case, select 'Open a New Support Case'. The case will be pre-populated with the portion of the log previously selected.</p>\n" +
    "\n" +
    "                            </div>\n" +
    "                        </div>\n" +
    "\n" +
    "                    </div>");
}]);

angular.module("log_viewer/views/navSideBar.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("log_viewer/views/navSideBar.html",
    "<div class=\"rha-navsidebar col-xs-3\" ng-class=\"{ showMe: sidePaneToggle }\" rha-filldown ng-style=\"{height: windowHeight }\">\n" +
    "    <div class=\"hideable-side-bar\" ng-class=\"{ showMe: sidePaneToggle }\">\n" +
    "        <div ng-controller=\"DropdownCtrl\" ng-init=\"init()\">\n" +
    "            <h4 class=\"file-list-title\" ng-class=\"{ showMe: hideDropdown}\" translate=''>Available Log Files</h4>\n" +
    "            <div class=\"btn-group\" ng-class=\"{ hideMe: hideDropdown}\">\n" +
    "                <div class=\"machines-spinner\" ng-class=\"{ showMe: loading }\">\n" +
    "                    <span class=\"rha-search-spinner pull-right\"></span>\n" +
    "                </div>\n" +
    "\n" +
    "                <button type=\"button\" class=\"dropdown-toggle btn btn-sm btn-primary\" data-toggle=\"dropdown\">\n" +
    "                    {{machinesDropdownText}}\n" +
    "                    <span class=\"caret\"></span>\n" +
    "                </button>\n" +
    "                <ul class=\"dropdown-menu\">\n" +
    "                    <li ng-repeat=\"choice in items\" ng-click=\"machineSelected()\"><a>{{choice}}</a>\n" +
    "                    </li>\n" +
    "                </ul>\n" +
    "            </div>\n" +
    "            <div ng-controller=\"fileController\">\n" +
    "                <div id=\"fileList\" rha-filldown ng-style=\"{ height: windowHeight }\" class=\"fileList\" >\n" +
    "                    <div ng-dblclick=\"selectItem(item)\" data-angular-treeview=\"true\" data-tree-id=\"mytree\" data-tree-model=\"roleList\" data-node-id=\"roleId\" data-node-label=\"roleName\" data-node-children=\"children\">\n" +
    "                    </div>\n" +
    "                </div>\n" +
    "                <button ng-disabled=\"retrieveFileButtonIsDisabled.check\" type=\"button\" class=\"pull-right btn btn-sm btn-primary\" ng-click=\"fileSelected()\" translate=''>\n" +
    "                    Select File</button>\n" +
    "            </div>\n" +
    "        </div>\n" +
    "    </div>\n" +
    "    <a ng-click=\"sidePaneToggle = !sidePaneToggle\">\n" +
    "        <span ng-class=\"{ showMe: sidePaneToggle }\" class=\"pull-right glyphicon glyphicon-chevron-left left-side-glyphicon\"></span>\n" +
    "    </a>\n" +
    "</div>");
}]);

angular.module("log_viewer/views/recommendations.html", []).run(["$templateCache", function($templateCache) {
  $templateCache.put("log_viewer/views/recommendations.html",
    "<div class=\"col-xs-6 pull-right solutions\" rha-filldown ng-style=\"{height: windowHeight }\" ng-class=\"{ showMe: showSolutions }\">\n" +
    "    <div id=\"resizeable-solution-view\" rha-filldown class=\"resizeable-solution-view\" ng-class=\"{ showMe: showSolutions }\" ng-style=\"{height: windowHeight }\" rha-accordionsearchresults='' opencase='true' ng-controller='SearchController'>\n" +
    "    </div>\n" +
    "    <a ng-click=\"toggleSolutions()\">\n" +
    "        <span ng-class=\"{ showMe: showSolutions }\" class=\"glyphicon glyphicon-chevron-left right-side-glyphicon\"></span>\n" +
    "    </a>\n" +
    "</div>");
}]);
