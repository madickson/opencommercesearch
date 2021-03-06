'use strict';

/**
 * @ngdoc function
 * @name relevancyApp.controller:SiteCtrl
 * @description
 * # SiteCtrl
 * Controller of the relevancyApp
 */
angular.module('relevancyApp').controller('SiteCtrl', function($scope, $rootScope, $stateParams, FIREBASE_ROOT, $firebase, $timeout, $window) {
    $scope.siteRef = new Firebase(FIREBASE_ROOT + '/sites/' + $stateParams.siteId);
    $scope.newCase = { alert: null };
    $firebase($scope.siteRef).$asObject().$bindTo($scope, 'site').then(function() {
        $rootScope.loading = '';
    });

    $scope.addCase = function(caseName) {
        caseName = caseName || '';

        var caseId = caseName.toLowerCase();

        $scope.newCase.alert = null;

        if ($scope.site.cases[caseId] || caseId === '' || caseId === false) {
            $scope.newCase.alert = {
                class: 'danger',
                type: 'ERROR',
                message: 'please use a different name'
            };
        } else {
            $scope.site.cases[caseId] = {
                '.priority': _.now() * -1,
                name: caseName
            };
            $scope.newCase.alert = {
                class: 'success',
                type: 'SUCCESS',
                message: 'case added'
            };
            $scope.newCase.name = '';

            $timeout(function() {
                $scope.newCase.alert = null;
            }, 5000);
        }
    };

    $scope.removeCase = function(caseId) {
        swal({
            title: 'Are you sure?',
            text: 'The case "' + caseId + '" will be permanently deleted!',
            type: 'error',
            showCancelButton: true,
            confirmButtonColor: '#DD6B55',
            confirmButtonText: 'Yes',
            closeOnConfirm: false
        },
        function(isConfirm) {
            if (isConfirm) {
                swal({
                    title: 'Deleted',
                    text: 'The case has been deleted.',
                    type: 'success',
                    confirmButtonColor: '#5cb85c'
                });
                $scope.siteRef.child('cases').child(caseId).remove();
            }
        });
    };
});