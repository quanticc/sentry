(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('PlayerCountDialogController', PlayerCountDialogController);

    PlayerCountDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'PlayerCount'];

    function PlayerCountDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, PlayerCount) {
        var vm = this;

        vm.playerCount = entity;
        vm.clear = clear;
        vm.datePickerOpenStatus = {};
        vm.openCalendar = openCalendar;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.playerCount.id !== null) {
                PlayerCount.update(vm.playerCount, onSaveSuccess, onSaveError);
            } else {
                PlayerCount.save(vm.playerCount, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:playerCountUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }

        vm.datePickerOpenStatus.timestamp = false;

        function openCalendar (date) {
            vm.datePickerOpenStatus[date] = true;
        }
    }
})();
