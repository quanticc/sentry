(function() {
    'use strict';

    angular
        .module('sentryApp')
        .controller('BotDialogController', BotDialogController);

    BotDialogController.$inject = ['$timeout', '$scope', '$stateParams', '$uibModalInstance', 'entity', 'Bot'];

    function BotDialogController ($timeout, $scope, $stateParams, $uibModalInstance, entity, Bot) {
        var vm = this;

        vm.bot = entity;
        vm.clear = clear;
        vm.save = save;

        $timeout(function (){
            angular.element('.form-group:eq(1)>input').focus();
        });

        function clear () {
            $uibModalInstance.dismiss('cancel');
        }

        function save () {
            vm.isSaving = true;
            if (vm.bot.id !== null) {
                Bot.update(vm.bot, onSaveSuccess, onSaveError);
            } else {
                Bot.save(vm.bot, onSaveSuccess, onSaveError);
            }
        }

        function onSaveSuccess (result) {
            $scope.$emit('sentryApp:botUpdate', result);
            $uibModalInstance.close(result);
            vm.isSaving = false;
        }

        function onSaveError () {
            vm.isSaving = false;
        }


    }
})();
