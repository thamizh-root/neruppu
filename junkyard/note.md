The title of this merge request should follow "New app: app name" format.Please make sure your fdroiddata fork is public and your branch is not protected. See https://docs.gitlab.com/user/project/repository/branches/protected/. 

We enable fast-forward merges by default. If your branch is protected, we can't rebase it before merge it.Please read our Git guide if you don't know how to rebase your branch. Don't rebase your branch if there is no conflict.Please read the guide first if this is your first contribution.

Please make sure your metadata follows the best practice in our templates.Please try your best to make sure all pipelines passed before open a merge request. If a test pipeline fails please check the log. Please check that the build pipeline does build your app. 

If the build pipeline succeeds but there is no APK files produced then you may have a mistake. Please check if you disable the build. Do not submit a metadata generated with fdroid import directly, please remove the disable line at least. Please check if the metadata file is in the correct path. 

It must be put in metadata/<applicationId>.yml.After all pipelines pass you can trigger the issue bot manually but do not trigger it too much which bloats the merge request.F-Droid CI runners are under GitLab's FOSS program, so there's no need for you to pay for any CI time. If Gitlab starts asking for phone numbers or credit cards don't submit anything, just leave a note in the MR so we know we need to trigger the CI.Please remove above lines!




https://gitlab.com/inboxtome26/fdroiddata/-/blob/0542d2a425c53118085101e26f2ee9df4de630f5/metadata/org.havenapp.neruppu.yml

https://gitlab.com/fdroid/fdroiddata/-/blob/master/metadata/org.havenapp.neruppu.yml

https://gitlab.com/fdroid/fdroiddata/-/merge_requests/41742
