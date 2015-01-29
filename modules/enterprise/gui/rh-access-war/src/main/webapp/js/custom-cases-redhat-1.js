var attachmentsRequest = 'attachments';

angular.module('RedhatAccess.JON', ['RedhatAccess.cases'])
.controller('customCase', ['$scope', 'NEW_DEFAULTS', '$location', '$http', function($scope, NEW_DEFAULTS , $location, $http) {
  NEW_DEFAULTS.product = "Red Hat JBoss Operations Network";
  NEW_DEFAULTS.version = "3.3.0";
  
  var params = $location.search();
  $http.defaults.headers.common['RHQ-Session-ID'] = params.sid;
  
  // handle support case for managed resource
  if ($location.path().indexOf('/resource-case') >= 0) {    
    NEW_DEFAULTS.product = params.product;
    NEW_DEFAULTS.version = params.version;
    attachmentsRequest += '?resourceId='+params.resourceId;
    $location.path('/case/new')
  }

}])
.run(['SECURITY_CONFIG', function(SECURITY_CONFIG) {
    SECURITY_CONFIG.forceLogin = true;
}]);
angular.module('RedhatAccess.cases')
  .controller('BackEndAttachmentsCtrl', ['$scope', 'TreeViewSelectorData', 'AttachmentsService',
    function ($scope, TreeViewSelectorData, AttachmentsService) {
      $scope.name = 'Attachments';
      $scope.attachmentTree = [];
      TreeViewSelectorData.getTree(attachmentsRequest).then(
        function (tree) {
          $scope.attachmentTree = tree;
          AttachmentsService.updateBackEndAttachments(tree);
        },
        function () {
          console.log('Unable to get tree data');
        });
    }
  ]);
  
