package com.jinhua.myRPC.service;

import com.jinhua.myRPC.common.Blog;

public class BlogServiceImpl implements BlogService{
    @Override
    public Blog getBlogById(Integer id) {
        Blog blog = Blog.builder().id(id).title("jinhua的博客").userId(22).build();
        System.out.println("客户端查询了"+id+"博客");
        return blog;
    }
}
