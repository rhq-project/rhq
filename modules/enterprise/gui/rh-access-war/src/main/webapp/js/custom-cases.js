angular.module('RedhatAccess.JON', ['RedhatAccess.cases'])
.controller('customCase', ['$scope', 'securityService', 'NEW_DEFAULTS', function($scope, securityService, NEW_DEFAULTS) {
	NEW_DEFAULTS.product = "Red Hat JBoss Operations Network";
	NEW_DEFAULTS.version = "3.3.0";

	 $scope.init = function () {
		 securityService.validateLogin(true);
	 };
}]);