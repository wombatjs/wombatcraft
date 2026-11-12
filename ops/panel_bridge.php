<?php
// Installed inside the existing panel container; no panel credentials leave it.
require '/app/vendor/autoload.php';
$app=require '/app/bootstrap/app.php';
$app->make(Illuminate\Contracts\Console\Kernel::class)->bootstrap();
$s=Pterodactyl\Models\Server::where('external_id','wombatcraft-production')->firstOrFail();
$action=$argv[1]??'info';
if ($action==='info') {echo json_encode(['uuid'=>$s->uuid,'id'=>$s->id,'status'=>$s->status]);}
elseif (in_array($action,['start','stop','restart'],true)) {$app->make(Pterodactyl\Repositories\Wings\DaemonPowerRepository::class)->setServer($s)->send($action);echo 'ok';}
elseif ($action==='command') {$app->make(Pterodactyl\Repositories\Wings\DaemonCommandRepository::class)->setServer($s)->send($argv[2]??'list');echo 'ok';}
else {throw new Exception('Unsupported action');}
